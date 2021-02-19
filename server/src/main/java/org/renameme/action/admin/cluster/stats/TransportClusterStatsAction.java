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

package org.renameme.action.admin.cluster.stats;

import org.apache.lucene.store.AlreadyClosedException;
import org.renameme.action.FailedNodeException;
import org.renameme.action.admin.cluster.node.info.NodeInfo;
import org.renameme.action.admin.cluster.node.stats.NodeStats;
import org.renameme.action.admin.indices.stats.CommonStats;
import org.renameme.action.admin.indices.stats.CommonStatsFlags;
import org.renameme.action.admin.indices.stats.ShardStats;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.nodes.BaseNodeRequest;
import org.renameme.action.support.nodes.TransportNodesAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.health.ClusterHealthStatus;
import org.renameme.cluster.health.ClusterStateHealth;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.common.io.stream.StreamOutput;
import org.renameme.index.IndexService;
import org.renameme.index.engine.CommitStats;
import org.renameme.index.seqno.RetentionLeaseStats;
import org.renameme.index.seqno.SeqNoStats;
import org.renameme.index.shard.IndexShard;
import org.renameme.indices.IndicesService;
import org.renameme.node.NodeService;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;
import org.renameme.transport.Transports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransportClusterStatsAction extends TransportNodesAction<ClusterStatsRequest, ClusterStatsResponse,
        TransportClusterStatsAction.ClusterStatsNodeRequest, ClusterStatsNodeResponse> {

    private static final CommonStatsFlags SHARD_STATS_FLAGS = new CommonStatsFlags(CommonStatsFlags.Flag.Docs, CommonStatsFlags.Flag.Store,
        CommonStatsFlags.Flag.FieldData, CommonStatsFlags.Flag.QueryCache,
        CommonStatsFlags.Flag.Completion, CommonStatsFlags.Flag.Segments);

    private final NodeService nodeService;
    private final IndicesService indicesService;


    @Inject
    public TransportClusterStatsAction(ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                       NodeService nodeService, IndicesService indicesService, ActionFilters actionFilters) {
        super(ClusterStatsAction.NAME, threadPool, clusterService, transportService, actionFilters, ClusterStatsRequest::new,
                ClusterStatsNodeRequest::new, ThreadPool.Names.MANAGEMENT, ThreadPool.Names.MANAGEMENT, ClusterStatsNodeResponse.class);
        this.nodeService = nodeService;
        this.indicesService = indicesService;
    }

    @Override
    protected ClusterStatsResponse newResponse(ClusterStatsRequest request,
                                               List<ClusterStatsNodeResponse> responses, List<FailedNodeException> failures) {
        assert Transports.assertNotTransportThread("Constructor of ClusterStatsResponse runs expensive computations on mappings found in" +
                " the cluster state that are too slow for a transport thread");
        ClusterState state = clusterService.state();
        return new ClusterStatsResponse(
            System.currentTimeMillis(),
            state.metadata().clusterUUID(),
            clusterService.getClusterName(),
            responses,
            failures,
            state);
    }

    @Override
    protected ClusterStatsNodeRequest newNodeRequest(ClusterStatsRequest request) {
        return new ClusterStatsNodeRequest(request);
    }

    @Override
    protected ClusterStatsNodeResponse newNodeResponse(StreamInput in) throws IOException {
        return new ClusterStatsNodeResponse(in);
    }

    @Override
    protected ClusterStatsNodeResponse nodeOperation(ClusterStatsNodeRequest nodeRequest) {
        NodeInfo nodeInfo = nodeService.info(true, true, false, true, false, true, false, true, false, false, false);
        NodeStats nodeStats = nodeService.stats(CommonStatsFlags.NONE,
                true, true, true, false, true, false, false, false, false, false, true, false, false, false);
        List<ShardStats> shardsStats = new ArrayList<>();
        for (IndexService indexService : indicesService) {
            for (IndexShard indexShard : indexService) {
                if (indexShard.routingEntry() != null && indexShard.routingEntry().active()) {
                    // only report on fully started shards
                    CommitStats commitStats;
                    SeqNoStats seqNoStats;
                    RetentionLeaseStats retentionLeaseStats;
                    try {
                        commitStats = indexShard.commitStats();
                        seqNoStats = indexShard.seqNoStats();
                        retentionLeaseStats = indexShard.getRetentionLeaseStats();
                    } catch (final AlreadyClosedException e) {
                        // shard is closed - no stats is fine
                        commitStats = null;
                        seqNoStats = null;
                        retentionLeaseStats = null;
                    }
                    shardsStats.add(
                            new ShardStats(
                                    indexShard.routingEntry(),
                                    indexShard.shardPath(),
                                    new CommonStats(indicesService.getIndicesQueryCache(), indexShard, SHARD_STATS_FLAGS),
                                    commitStats,
                                    seqNoStats,
                                    retentionLeaseStats));
                }
            }
        }

        ClusterHealthStatus clusterStatus = null;
        if (clusterService.state().nodes().isLocalNodeElectedMaster()) {
            clusterStatus = new ClusterStateHealth(clusterService.state()).getStatus();
        }

        return new ClusterStatsNodeResponse(nodeInfo.getNode(), clusterStatus, nodeInfo, nodeStats,
            shardsStats.toArray(new ShardStats[shardsStats.size()]));

    }

    public static class ClusterStatsNodeRequest extends BaseNodeRequest {

        ClusterStatsRequest request;

        public ClusterStatsNodeRequest(StreamInput in) throws IOException {
            super(in);
            request = new ClusterStatsRequest(in);
        }

        ClusterStatsNodeRequest(ClusterStatsRequest request) {
            this.request = request;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            request.writeTo(out);
        }
    }
}
