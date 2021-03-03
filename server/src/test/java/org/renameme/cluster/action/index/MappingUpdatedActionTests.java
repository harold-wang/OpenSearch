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
package org.renameme.cluster.action.index;

import org.renameme.LegacyESVersion;
import org.renameme.Version;
import org.renameme.action.ActionListener;
import org.renameme.action.admin.indices.mapping.put.AutoPutMappingAction;
import org.renameme.action.support.PlainActionFuture;
import org.renameme.client.AdminClient;
import org.renameme.client.Client;
import org.renameme.client.IndicesAdminClient;
import org.renameme.cluster.ClusterName;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.action.index.MappingUpdatedAction.AdjustableSemaphore;
import org.renameme.cluster.node.DiscoveryNode;
import org.renameme.cluster.node.DiscoveryNodes;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.collect.Map;
import org.renameme.common.settings.ClusterSettings;
import org.renameme.common.settings.Settings;
import org.renameme.index.Index;
import org.renameme.index.mapper.ContentPath;
import org.renameme.index.mapper.Mapper;
import org.renameme.index.mapper.Mapping;
import org.renameme.index.mapper.MetadataFieldMapper;
import org.renameme.index.mapper.RootObjectMapper;
import org.renameme.test.ESTestCase;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.renameme.cluster.metadata.IndexMetadata.SETTING_VERSION_CREATED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MappingUpdatedActionTests extends ESTestCase {

    public void testAdjustableSemaphore() {
        AdjustableSemaphore sem = new AdjustableSemaphore(1, randomBoolean());
        assertEquals(1, sem.availablePermits());
        assertTrue(sem.tryAcquire());
        assertEquals(0, sem.availablePermits());
        assertFalse(sem.tryAcquire());
        assertEquals(0, sem.availablePermits());

        // increase the number of max permits to 2
        sem.setMaxPermits(2);
        assertEquals(1, sem.availablePermits());
        assertTrue(sem.tryAcquire());
        assertEquals(0, sem.availablePermits());

        // release all current permits
        sem.release();
        assertEquals(1, sem.availablePermits());
        sem.release();
        assertEquals(2, sem.availablePermits());

        // reduce number of max permits to 1
        sem.setMaxPermits(1);
        assertEquals(1, sem.availablePermits());
        // set back to 2
        sem.setMaxPermits(2);
        assertEquals(2, sem.availablePermits());

        // take both permits and reduce max permits
        assertTrue(sem.tryAcquire());
        assertTrue(sem.tryAcquire());
        assertEquals(0, sem.availablePermits());
        assertFalse(sem.tryAcquire());
        sem.setMaxPermits(1);
        assertEquals(-1, sem.availablePermits());
        assertFalse(sem.tryAcquire());

        // release one permit
        sem.release();
        assertEquals(0, sem.availablePermits());
        assertFalse(sem.tryAcquire());

        // release second permit
        sem.release();
        assertEquals(1, sem.availablePermits());
        assertTrue(sem.tryAcquire());
    }

    public void testMappingUpdatedActionBlocks() throws Exception {
        List<ActionListener<Void>> inFlightListeners = new CopyOnWriteArrayList<>();
        final MappingUpdatedAction mua = new MappingUpdatedAction(Settings.builder()
            .put(MappingUpdatedAction.INDICES_MAX_IN_FLIGHT_UPDATES_SETTING.getKey(), 1).build(),
            new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS), null) {

            @Override
            protected void sendUpdateMapping(Index index, String type, Mapping mappingUpdate, ActionListener<Void> listener) {
                inFlightListeners.add(listener);
            }
        };

        PlainActionFuture<Void> fut1 = new PlainActionFuture<>();
        mua.updateMappingOnMaster(null, "test", null, fut1);
        assertEquals(1, inFlightListeners.size());
        assertEquals(0, mua.blockedThreads());

        PlainActionFuture<Void> fut2 = new PlainActionFuture<>();
        Thread thread = new Thread(() -> {
            mua.updateMappingOnMaster(null, "test", null, fut2); // blocked
        });
        thread.start();
        assertBusy(() -> assertEquals(1, mua.blockedThreads()));

        assertEquals(1, inFlightListeners.size());
        assertFalse(fut1.isDone());
        inFlightListeners.remove(0).onResponse(null);
        assertTrue(fut1.isDone());

        thread.join();
        assertEquals(0, mua.blockedThreads());
        assertEquals(1, inFlightListeners.size());
        assertFalse(fut2.isDone());
        inFlightListeners.remove(0).onResponse(null);
        assertTrue(fut2.isDone());
    }

    public void testSendUpdateMappingUsingPutMappingAction() {
        DiscoveryNodes nodes = DiscoveryNodes.builder()
            .add(new DiscoveryNode("first", buildNewFakeTransportAddress(), LegacyESVersion.V_7_8_0))
            .build();
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).nodes(nodes).build();
        ClusterService clusterService = mock(ClusterService.class);
        when(clusterService.state()).thenReturn(clusterState);

        IndicesAdminClient indicesAdminClient = mock(IndicesAdminClient.class);
        AdminClient adminClient = mock(AdminClient.class);
        when(adminClient.indices()).thenReturn(indicesAdminClient);
        Client client = mock(Client.class);
        when(client.admin()).thenReturn(adminClient);

        MappingUpdatedAction mua = new MappingUpdatedAction(Settings.EMPTY,
            new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS), clusterService);
        mua.setClient(client);

        Settings indexSettings = Settings.builder().put(SETTING_VERSION_CREATED, Version.CURRENT).build();
        final Mapper.BuilderContext context = new Mapper.BuilderContext(indexSettings, new ContentPath());
        RootObjectMapper rootObjectMapper = new RootObjectMapper.Builder("name").build(context);
        Mapping update = new Mapping(LegacyESVersion.V_7_8_0, rootObjectMapper, new MetadataFieldMapper[0], Map.of());

        mua.sendUpdateMapping(new Index("name", "uuid"), "type", update, ActionListener.wrap(() -> {}));
        verify(indicesAdminClient).putMapping(any(), any());
    }

    public void testSendUpdateMappingUsingAutoPutMappingAction() {
        DiscoveryNodes nodes = DiscoveryNodes.builder()
            .add(new DiscoveryNode("first", buildNewFakeTransportAddress(), LegacyESVersion.V_7_9_0))
            .build();
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).nodes(nodes).build();
        ClusterService clusterService = mock(ClusterService.class);
        when(clusterService.state()).thenReturn(clusterState);

        IndicesAdminClient indicesAdminClient = mock(IndicesAdminClient.class);
        AdminClient adminClient = mock(AdminClient.class);
        when(adminClient.indices()).thenReturn(indicesAdminClient);
        Client client = mock(Client.class);
        when(client.admin()).thenReturn(adminClient);

        MappingUpdatedAction mua = new MappingUpdatedAction(Settings.EMPTY,
            new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS), clusterService);
        mua.setClient(client);

        Settings indexSettings = Settings.builder().put(SETTING_VERSION_CREATED, Version.CURRENT).build();
        final Mapper.BuilderContext context = new Mapper.BuilderContext(indexSettings, new ContentPath());
        RootObjectMapper rootObjectMapper = new RootObjectMapper.Builder("name").build(context);
        Mapping update = new Mapping(LegacyESVersion.V_7_9_0, rootObjectMapper, new MetadataFieldMapper[0], Map.of());

        mua.sendUpdateMapping(new Index("name", "uuid"), "type", update, ActionListener.wrap(() -> {}));
        verify(indicesAdminClient).execute(eq(AutoPutMappingAction.INSTANCE), any(), any());
    }
}
