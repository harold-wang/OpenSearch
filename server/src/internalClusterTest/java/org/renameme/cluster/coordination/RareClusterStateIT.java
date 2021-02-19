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

package org.renameme.cluster.coordination;

import org.renameme.RenamemeParseException;
import org.renameme.Version;
import org.renameme.action.ActionFuture;
import org.renameme.action.ActionRequest;
import org.renameme.action.ActionRequestBuilder;
import org.renameme.action.ActionResponse;
import org.renameme.action.index.IndexResponse;
import org.renameme.action.support.master.AcknowledgedResponse;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.ClusterStateUpdateTask;
import org.renameme.cluster.block.ClusterBlocks;
import org.renameme.cluster.metadata.IndexMetadata;
import org.renameme.cluster.metadata.MappingMetadata;
import org.renameme.cluster.metadata.Metadata;
import org.renameme.cluster.node.DiscoveryNode;
import org.renameme.cluster.node.DiscoveryNodes;
import org.renameme.cluster.routing.RoutingTable;
import org.renameme.cluster.routing.ShardRouting;
import org.renameme.cluster.routing.allocation.AllocationService;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.collect.ImmutableOpenMap;
import org.renameme.common.settings.Settings;
import org.renameme.common.unit.TimeValue;
import org.renameme.discovery.Discovery;
import org.renameme.index.Index;
import org.renameme.index.IndexService;
import org.renameme.index.mapper.DocumentMapper;
import org.renameme.index.mapper.MapperService;
import org.renameme.indices.IndicesService;
import org.renameme.test.ESIntegTestCase;
import org.renameme.test.disruption.BlockClusterStateProcessing;
import org.renameme.transport.TransportSettings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.renameme.action.DocWriteResponse.Result.CREATED;
import static org.renameme.test.hamcrest.RenamemeAssertions.assertAcked;
import static org.renameme.test.hamcrest.RenamemeAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0, numClientNodes = 0, transportClientRatio = 0)
public class RareClusterStateIT extends ESIntegTestCase {

    @Override
    protected int numberOfShards() {
        return 1;
    }

    @Override
    protected int numberOfReplicas() {
        return 0;
    }

    public void testAssignmentWithJustAddedNodes() {
        internalCluster().startNode(Settings.builder().put(TransportSettings.CONNECT_TIMEOUT.getKey(), "1s"));
        final String index = "index";
        prepareCreate(index).setSettings(Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
            .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)).get();
        ensureGreen(index);

        // close to have some unassigned started shards shards..
        client().admin().indices().prepareClose(index).get();

        final String masterName = internalCluster().getMasterName();
        final ClusterService clusterService = internalCluster().clusterService(masterName);
        final AllocationService allocationService = internalCluster().getInstance(AllocationService.class, masterName);
        clusterService.submitStateUpdateTask("test-inject-node-and-reroute", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                // inject a node
                ClusterState.Builder builder = ClusterState.builder(currentState);
                builder.nodes(DiscoveryNodes.builder(currentState.nodes()).add(new DiscoveryNode("_non_existent",
                        buildNewFakeTransportAddress(), emptyMap(), emptySet(), Version.CURRENT)));

                // open index
                final IndexMetadata indexMetadata = IndexMetadata.builder(currentState.metadata()
                    .index(index)).state(IndexMetadata.State.OPEN).build();

                builder.metadata(Metadata.builder(currentState.metadata()).put(indexMetadata, true));
                builder.blocks(ClusterBlocks.builder().blocks(currentState.blocks()).removeIndexBlocks(index));
                ClusterState updatedState = builder.build();

                RoutingTable.Builder routingTable = RoutingTable.builder(updatedState.routingTable());
                routingTable.addAsRecovery(updatedState.metadata().index(index));
                updatedState = ClusterState.builder(updatedState).routingTable(routingTable.build()).build();

                return allocationService.reroute(updatedState, "reroute");
            }

            @Override
            public void onFailure(String source, Exception e) {
            }
        });
        ensureGreen(index);
        // remove the extra node
        clusterService.submitStateUpdateTask("test-remove-injected-node", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                ClusterState.Builder builder = ClusterState.builder(currentState);
                builder.nodes(DiscoveryNodes.builder(currentState.nodes()).remove("_non_existent"));

                currentState = builder.build();
                return allocationService.disassociateDeadNodes(currentState, true, "reroute");
            }

            @Override
            public void onFailure(String source, Exception e) {
            }
        });
    }

    private <Req extends ActionRequest, Res extends ActionResponse> ActionFuture<Res> executeAndCancelCommittedPublication(
            ActionRequestBuilder<Req, Res> req) throws Exception {
        // Wait for no publication in progress to not accidentally cancel a publication different from the one triggered by the given
        // request.
        final Coordinator masterCoordinator = (Coordinator) internalCluster().getCurrentMasterNodeInstance(Discovery.class);
        assertBusy(() -> {
            assertFalse(masterCoordinator.publicationInProgress());
            final long applierVersion = masterCoordinator.getApplierState().version();
            for (Discovery instance : internalCluster().getInstances(Discovery.class)) {
                assertEquals(((Coordinator) instance).getApplierState().version(), applierVersion);
            }
        });
        ActionFuture<Res> future = req.execute();
        assertBusy(() -> assertTrue(masterCoordinator.cancelCommittedPublication()));
        return future;
    }

    public void testDeleteCreateInOneBulk() throws Exception {
        internalCluster().startMasterOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        assertFalse(client().admin().cluster().prepareHealth().setWaitForNodes("2").get().isTimedOut());
        prepareCreate("test").setSettings(Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)).addMapping("type").get();
        ensureGreen("test");

        // block none master node.
        BlockClusterStateProcessing disruption = new BlockClusterStateProcessing(dataNode, random());
        internalCluster().setDisruptionScheme(disruption);
        logger.info("--> indexing a doc");
        index("test", "type", "1");
        refresh();
        disruption.startDisrupting();
        logger.info("--> delete index and recreate it");
        executeAndCancelCommittedPublication(client().admin().indices().prepareDelete("test").setTimeout("0s"))
                .get(10, TimeUnit.SECONDS);
        executeAndCancelCommittedPublication(prepareCreate("test").setSettings(Settings.builder().put(IndexMetadata
                .SETTING_NUMBER_OF_REPLICAS, 0).put(IndexMetadata.SETTING_WAIT_FOR_ACTIVE_SHARDS.getKey(), "0")).setTimeout("0s"))
                .get(10, TimeUnit.SECONDS);

        logger.info("--> letting cluster proceed");

        disruption.stopDisrupting();
        ensureGreen(TimeValue.timeValueMinutes(30), "test");
        // due to publish_timeout of 0, wait for data node to have cluster state fully applied
        assertBusy(() -> {
                long masterClusterStateVersion = internalCluster().clusterService(internalCluster().getMasterName()).state().version();
                long dataClusterStateVersion = internalCluster().clusterService(dataNode).state().version();
                assertThat(masterClusterStateVersion, equalTo(dataClusterStateVersion));
            });
        assertHitCount(client().prepareSearch("test").get(), 0);
    }

    public void testDelayedMappingPropagationOnPrimary() throws Exception {
        // Here we want to test that things go well if there is a first request
        // that adds mappings but before mappings are propagated to all nodes
        // another index request introduces the same mapping. The master node
        // will reply immediately since it did not change the cluster state
        // but the change might not be on the node that performed the indexing
        // operation yet

        final List<String> nodeNames = internalCluster().startNodes(2);
        assertFalse(client().admin().cluster().prepareHealth().setWaitForNodes("2").get().isTimedOut());

        final String master = internalCluster().getMasterName();
        assertThat(nodeNames, hasItem(master));
        String otherNode = null;
        for (String node : nodeNames) {
            if (node.equals(master) == false) {
                otherNode = node;
                break;
            }
        }
        assertNotNull(otherNode);

        // Don't allocate the shard on the master node
        assertAcked(prepareCreate("index").setSettings(Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put("index.routing.allocation.exclude._name", master)).get());
        ensureGreen();

        // Check routing tables
        ClusterState state = client().admin().cluster().prepareState().get().getState();
        assertEquals(master, state.nodes().getMasterNode().getName());
        List<ShardRouting> shards = state.routingTable().allShards("index");
        assertThat(shards, hasSize(1));
        for (ShardRouting shard : shards) {
            if (shard.primary()) {
                // primary must not be on the master node
                assertFalse(state.nodes().getMasterNodeId().equals(shard.currentNodeId()));
            } else {
                fail(); // only primaries
            }
        }

        // Block cluster state processing where our shard is
        BlockClusterStateProcessing disruption = new BlockClusterStateProcessing(otherNode, random());
        internalCluster().setDisruptionScheme(disruption);
        disruption.startDisrupting();

        // Add a new mapping...
        ActionFuture<AcknowledgedResponse> putMappingResponse =
                executeAndCancelCommittedPublication(client().admin().indices().preparePutMapping("index")
                .setType("type").setSource("field", "type=long"));

        // ...and wait for mappings to be available on master
        assertBusy(() -> {
            ImmutableOpenMap<String, MappingMetadata> indexMappings = client().admin().indices()
                .prepareGetMappings("index").get().getMappings().get("index");
            assertNotNull(indexMappings);
            MappingMetadata typeMappings = indexMappings.get("type");
            assertNotNull(typeMappings);
            Object properties;
            try {
                properties = typeMappings.getSourceAsMap().get("properties");
            } catch (RenamemeParseException e) {
                throw new AssertionError(e);
            }
            assertNotNull(properties);
            Object fieldMapping = ((Map<String, Object>) properties).get("field");
            assertNotNull(fieldMapping);
        });

        // this request does not change the cluster state, because mapping is already created,
        // we don't await and cancel committed publication
        ActionFuture<IndexResponse> docIndexResponse =
                client().prepareIndex("index", "type", "1").setSource("field", 42).execute();

        // Wait a bit to make sure that the reason why we did not get a response
        // is that cluster state processing is blocked and not just that it takes
        // time to process the indexing request
        Thread.sleep(100);
        assertFalse(putMappingResponse.isDone());
        assertFalse(docIndexResponse.isDone());

        // Now make sure the indexing request finishes successfully
        disruption.stopDisrupting();
        assertTrue(putMappingResponse.get(10, TimeUnit.SECONDS).isAcknowledged());
        assertThat(docIndexResponse.get(10, TimeUnit.SECONDS), instanceOf(IndexResponse.class));
        assertEquals(1, docIndexResponse.get(10, TimeUnit.SECONDS).getShardInfo().getTotal());
    }

    public void testDelayedMappingPropagationOnReplica() throws Exception {
        // This is essentially the same thing as testDelayedMappingPropagationOnPrimary
        // but for replicas
        // Here we want to test that everything goes well if the mappings that
        // are needed for a document are not available on the replica at the
        // time of indexing it
        final List<String> nodeNames = internalCluster().startNodes(2);
        assertFalse(client().admin().cluster().prepareHealth().setWaitForNodes("2").get().isTimedOut());

        final String master = internalCluster().getMasterName();
        assertThat(nodeNames, hasItem(master));
        String otherNode = null;
        for (String node : nodeNames) {
            if (node.equals(master) == false) {
                otherNode = node;
                break;
            }
        }
        assertNotNull(otherNode);

        // Force allocation of the primary on the master node by first only allocating on the master
        // and then allowing all nodes so that the replica gets allocated on the other node
        prepareCreate("index").setSettings(Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 1)
                .put("index.routing.allocation.include._name", master)).get();
        client().admin().indices().prepareUpdateSettings("index").setSettings(Settings.builder()
                .put("index.routing.allocation.include._name", "")).get();
        ensureGreen();

        // Check routing tables
        ClusterState state = client().admin().cluster().prepareState().get().getState();
        assertEquals(master, state.nodes().getMasterNode().getName());
        List<ShardRouting> shards = state.routingTable().allShards("index");
        assertThat(shards, hasSize(2));
        for (ShardRouting shard : shards) {
            if (shard.primary()) {
                // primary must be on the master
                assertEquals(state.nodes().getMasterNodeId(), shard.currentNodeId());
            } else {
                assertTrue(shard.active());
            }
        }

        // Block cluster state processing on the replica
        BlockClusterStateProcessing disruption = new BlockClusterStateProcessing(otherNode, random());
        internalCluster().setDisruptionScheme(disruption);
        disruption.startDisrupting();
        final ActionFuture<AcknowledgedResponse> putMappingResponse =
                executeAndCancelCommittedPublication(client().admin().indices().preparePutMapping("index")
                .setType("type").setSource("field", "type=long"));

        final Index index = resolveIndex("index");
        // Wait for mappings to be available on master
        assertBusy(() -> {
            final IndicesService indicesService = internalCluster().getInstance(IndicesService.class, master);
            final IndexService indexService = indicesService.indexServiceSafe(index);
            assertNotNull(indexService);
            final MapperService mapperService = indexService.mapperService();
            DocumentMapper mapper = mapperService.documentMapper("type");
            assertNotNull(mapper);
            assertNotNull(mapper.mappers().getMapper("field"));
        });

        final ActionFuture<IndexResponse> docIndexResponse = client().prepareIndex("index", "type", "1").setSource("field", 42).execute();

        assertBusy(() -> assertTrue(client().prepareGet("index", "type", "1").get().isExists()));

        // index another document, this time using dynamic mappings.
        // The ack timeout of 0 on dynamic mapping updates makes it possible for the document to be indexed on the primary, even
        // if the dynamic mapping update is not applied on the replica yet.
        // this request does not change the cluster state, because the mapping is dynamic,
        // we need to await and cancel committed publication
        ActionFuture<IndexResponse> dynamicMappingsFut =
                executeAndCancelCommittedPublication(client().prepareIndex("index", "type", "2").setSource("field2", 42));

        // ...and wait for second mapping to be available on master
        assertBusy(() -> {
            final IndicesService indicesService = internalCluster().getInstance(IndicesService.class, master);
            final IndexService indexService = indicesService.indexServiceSafe(index);
            assertNotNull(indexService);
            final MapperService mapperService = indexService.mapperService();
            DocumentMapper mapper = mapperService.documentMapper("type");
            assertNotNull(mapper);
            assertNotNull(mapper.mappers().getMapper("field2"));
        });

        assertBusy(() -> assertTrue(client().prepareGet("index", "type", "2").get().isExists()));

        // The mappings have not been propagated to the replica yet as a consequence the document count not be indexed
        // We wait on purpose to make sure that the document is not indexed because the shard operation is stalled
        // and not just because it takes time to replicate the indexing request to the replica
        Thread.sleep(100);
        assertFalse(putMappingResponse.isDone());
        assertFalse(docIndexResponse.isDone());

        // Now make sure the indexing request finishes successfully
        disruption.stopDisrupting();
        assertTrue(putMappingResponse.get(10, TimeUnit.SECONDS).isAcknowledged());
        assertThat(docIndexResponse.get(10, TimeUnit.SECONDS), instanceOf(IndexResponse.class));
        assertEquals(2, docIndexResponse.get(10, TimeUnit.SECONDS).getShardInfo().getTotal()); // both shards should have succeeded

        assertThat(dynamicMappingsFut.get(10, TimeUnit.SECONDS).getResult(), equalTo(CREATED));
    }

}
