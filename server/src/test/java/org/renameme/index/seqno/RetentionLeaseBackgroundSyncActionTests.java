/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.renameme.index.seqno;

import org.renameme.action.ActionListener;
import org.renameme.action.LatchedActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.ActionTestUtils;
import org.renameme.action.support.PlainActionFuture;
import org.renameme.action.support.replication.TransportReplicationAction;
import org.renameme.cluster.action.shard.ShardStateAction;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.settings.Settings;
import org.renameme.core.internal.io.IOUtils;
import org.renameme.gateway.WriteStateException;
import org.renameme.index.Index;
import org.renameme.index.IndexService;
import org.renameme.index.shard.IndexShard;
import org.renameme.index.shard.ShardId;
import org.renameme.indices.IndicesService;
import org.renameme.test.ESTestCase;
import org.renameme.test.transport.CapturingTransport;
import org.renameme.threadpool.TestThreadPool;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.mock.orig.Mockito.when;
import static org.renameme.test.ClusterServiceUtils.createClusterService;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RetentionLeaseBackgroundSyncActionTests extends ESTestCase {

    private ThreadPool threadPool;
    private CapturingTransport transport;
    private ClusterService clusterService;
    private TransportService transportService;
    private ShardStateAction shardStateAction;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool(getClass().getName());
        transport = new CapturingTransport();
        clusterService = createClusterService(threadPool);
        transportService = transport.createTransportService(
                clusterService.getSettings(),
                threadPool,
                TransportService.NOOP_TRANSPORT_INTERCEPTOR,
                boundAddress -> clusterService.localNode(),
                null,
                Collections.emptySet());
        transportService.start();
        transportService.acceptIncomingRequests();
        shardStateAction = new ShardStateAction(clusterService, transportService, null, null, threadPool);
    }

    @Override
    public void tearDown() throws Exception {
        try {
            IOUtils.close(transportService, clusterService, transport);
        } finally {
            terminate(threadPool);
        }
        super.tearDown();
    }

    public void testRetentionLeaseBackgroundSyncActionOnPrimary() throws InterruptedException {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final RetentionLeaseBackgroundSyncAction action = new RetentionLeaseBackgroundSyncAction(
                Settings.EMPTY,
                transportService,
                clusterService,
                indicesService,
                threadPool,
                shardStateAction,
                new ActionFilters(Collections.emptySet()));
        final RetentionLeases retentionLeases = mock(RetentionLeases.class);
        final RetentionLeaseBackgroundSyncAction.Request request =
                new RetentionLeaseBackgroundSyncAction.Request(indexShard.shardId(), retentionLeases);

        final CountDownLatch latch = new CountDownLatch(1);
        action.shardOperationOnPrimary(request, indexShard,
            new LatchedActionListener<>(ActionTestUtils.assertNoFailureListener(result -> {
                // the retention leases on the shard should be persisted
                verify(indexShard).persistRetentionLeases();
                // we should forward the request containing the current retention leases to the replica
                assertThat(result.replicaRequest(), sameInstance(request));
            }), latch));
        latch.await();
    }

    public void testRetentionLeaseBackgroundSyncActionOnReplica() throws WriteStateException {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final RetentionLeaseBackgroundSyncAction action = new RetentionLeaseBackgroundSyncAction(
                Settings.EMPTY,
                transportService,
                clusterService,
                indicesService,
                threadPool,
                shardStateAction,
                new ActionFilters(Collections.emptySet()));
        final RetentionLeases retentionLeases = mock(RetentionLeases.class);
        final RetentionLeaseBackgroundSyncAction.Request request =
                new RetentionLeaseBackgroundSyncAction.Request(indexShard.shardId(), retentionLeases);

        final PlainActionFuture<TransportReplicationAction.ReplicaResult> listener = PlainActionFuture.newFuture();
        action.shardOperationOnReplica(request, indexShard, listener);
        final TransportReplicationAction.ReplicaResult result = listener.actionGet();
        // the retention leases on the shard should be updated
        verify(indexShard).updateRetentionLeasesOnReplica(retentionLeases);
        // the retention leases on the shard should be persisted
        verify(indexShard).persistRetentionLeases();
        // the result should indicate success
        final AtomicBoolean success = new AtomicBoolean();
        result.runPostReplicaActions(ActionListener.wrap(r -> success.set(true), e -> fail(e.toString())));
        assertTrue(success.get());
    }

    public void testBlocks() {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final RetentionLeaseBackgroundSyncAction action = new RetentionLeaseBackgroundSyncAction(
                Settings.EMPTY,
                transportService,
                clusterService,
                indicesService,
                threadPool,
                shardStateAction,
                new ActionFilters(Collections.emptySet()));

        assertNull(action.indexBlockLevel());
    }

}
