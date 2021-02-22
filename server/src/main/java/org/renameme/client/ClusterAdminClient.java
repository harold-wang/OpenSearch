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

package org.renameme.client;

import org.renameme.action.ActionFuture;
import org.renameme.action.ActionListener;
import org.renameme.action.admin.cluster.allocation.ClusterAllocationExplainRequest;
import org.renameme.action.admin.cluster.allocation.ClusterAllocationExplainRequestBuilder;
import org.renameme.action.admin.cluster.allocation.ClusterAllocationExplainResponse;
import org.renameme.action.admin.cluster.health.ClusterHealthRequest;
import org.renameme.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.renameme.action.admin.cluster.health.ClusterHealthResponse;
import org.renameme.action.admin.cluster.node.hotthreads.NodesHotThreadsRequest;
import org.renameme.action.admin.cluster.node.hotthreads.NodesHotThreadsRequestBuilder;
import org.renameme.action.admin.cluster.node.hotthreads.NodesHotThreadsResponse;
import org.renameme.action.admin.cluster.node.info.NodesInfoRequest;
import org.renameme.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import org.renameme.action.admin.cluster.node.info.NodesInfoResponse;
import org.renameme.action.admin.cluster.node.reload.NodesReloadSecureSettingsRequestBuilder;
import org.renameme.action.admin.cluster.node.stats.NodesStatsRequest;
import org.renameme.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.renameme.action.admin.cluster.node.stats.NodesStatsResponse;
import org.renameme.action.admin.cluster.node.tasks.cancel.CancelTasksRequest;
import org.renameme.action.admin.cluster.node.tasks.cancel.CancelTasksRequestBuilder;
import org.renameme.action.admin.cluster.node.tasks.cancel.CancelTasksResponse;
import org.renameme.action.admin.cluster.node.tasks.get.GetTaskRequest;
import org.renameme.action.admin.cluster.node.tasks.get.GetTaskRequestBuilder;
import org.renameme.action.admin.cluster.node.tasks.get.GetTaskResponse;
import org.renameme.action.admin.cluster.node.tasks.list.ListTasksRequest;
import org.renameme.action.admin.cluster.node.tasks.list.ListTasksRequestBuilder;
import org.renameme.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.renameme.action.admin.cluster.node.usage.NodesUsageRequest;
import org.renameme.action.admin.cluster.node.usage.NodesUsageRequestBuilder;
import org.renameme.action.admin.cluster.node.usage.NodesUsageResponse;
import org.renameme.action.admin.cluster.repositories.cleanup.CleanupRepositoryRequest;
import org.renameme.action.admin.cluster.repositories.cleanup.CleanupRepositoryRequestBuilder;
import org.renameme.action.admin.cluster.repositories.cleanup.CleanupRepositoryResponse;
import org.renameme.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.renameme.action.admin.cluster.repositories.delete.DeleteRepositoryRequestBuilder;
import org.renameme.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.renameme.action.admin.cluster.repositories.get.GetRepositoriesRequestBuilder;
import org.renameme.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.renameme.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.renameme.action.admin.cluster.repositories.put.PutRepositoryRequestBuilder;
import org.renameme.action.admin.cluster.repositories.verify.VerifyRepositoryRequest;
import org.renameme.action.admin.cluster.repositories.verify.VerifyRepositoryRequestBuilder;
import org.renameme.action.admin.cluster.repositories.verify.VerifyRepositoryResponse;
import org.renameme.action.admin.cluster.reroute.ClusterRerouteRequest;
import org.renameme.action.admin.cluster.reroute.ClusterRerouteRequestBuilder;
import org.renameme.action.admin.cluster.reroute.ClusterRerouteResponse;
import org.renameme.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.renameme.action.admin.cluster.settings.ClusterUpdateSettingsRequestBuilder;
import org.renameme.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.renameme.action.admin.cluster.shards.ClusterSearchShardsRequest;
import org.renameme.action.admin.cluster.shards.ClusterSearchShardsRequestBuilder;
import org.renameme.action.admin.cluster.shards.ClusterSearchShardsResponse;
import org.renameme.action.admin.cluster.snapshots.clone.CloneSnapshotRequest;
import org.renameme.action.admin.cluster.snapshots.clone.CloneSnapshotRequestBuilder;
import org.renameme.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.renameme.action.admin.cluster.snapshots.create.CreateSnapshotRequestBuilder;
import org.renameme.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.renameme.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.renameme.action.admin.cluster.snapshots.delete.DeleteSnapshotRequestBuilder;
import org.renameme.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.renameme.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import org.renameme.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.renameme.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.renameme.action.admin.cluster.snapshots.restore.RestoreSnapshotRequestBuilder;
import org.renameme.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.renameme.action.admin.cluster.snapshots.status.SnapshotsStatusRequest;
import org.renameme.action.admin.cluster.snapshots.status.SnapshotsStatusRequestBuilder;
import org.renameme.action.admin.cluster.snapshots.status.SnapshotsStatusResponse;
import org.renameme.action.admin.cluster.state.ClusterStateRequest;
import org.renameme.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.renameme.action.admin.cluster.state.ClusterStateResponse;
import org.renameme.action.admin.cluster.stats.ClusterStatsRequest;
import org.renameme.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.renameme.action.admin.cluster.stats.ClusterStatsResponse;
import org.renameme.action.admin.cluster.storedscripts.DeleteStoredScriptRequest;
import org.renameme.action.admin.cluster.storedscripts.DeleteStoredScriptRequestBuilder;
import org.renameme.action.admin.cluster.storedscripts.GetStoredScriptRequest;
import org.renameme.action.admin.cluster.storedscripts.GetStoredScriptRequestBuilder;
import org.renameme.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import org.renameme.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.renameme.action.admin.cluster.storedscripts.PutStoredScriptRequestBuilder;
import org.renameme.action.admin.cluster.tasks.PendingClusterTasksRequest;
import org.renameme.action.admin.cluster.tasks.PendingClusterTasksRequestBuilder;
import org.renameme.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.renameme.action.admin.indices.dangling.delete.DeleteDanglingIndexRequest;
import org.renameme.action.admin.indices.dangling.import_index.ImportDanglingIndexRequest;
import org.renameme.action.admin.indices.dangling.list.ListDanglingIndicesRequest;
import org.renameme.action.admin.indices.dangling.list.ListDanglingIndicesResponse;
import org.renameme.action.ingest.DeletePipelineRequest;
import org.renameme.action.ingest.DeletePipelineRequestBuilder;
import org.renameme.action.ingest.GetPipelineRequest;
import org.renameme.action.ingest.GetPipelineRequestBuilder;
import org.renameme.action.ingest.GetPipelineResponse;
import org.renameme.action.ingest.PutPipelineRequest;
import org.renameme.action.ingest.PutPipelineRequestBuilder;
import org.renameme.action.ingest.SimulatePipelineRequest;
import org.renameme.action.ingest.SimulatePipelineRequestBuilder;
import org.renameme.action.ingest.SimulatePipelineResponse;
import org.renameme.action.support.master.AcknowledgedResponse;
import org.renameme.common.bytes.BytesReference;
import org.renameme.common.xcontent.XContentType;
import org.renameme.tasks.TaskId;

/**
 * Administrative actions/operations against indices.
 *
 * @see AdminClient#cluster()
 */
public interface ClusterAdminClient extends RenamemeClient {

    /**
     * The health of the cluster.
     *
     * @param request The cluster state request
     * @return The result future
     * @see Requests#clusterHealthRequest(String...)
     */
    ActionFuture<ClusterHealthResponse> health(ClusterHealthRequest request);

    /**
     * The health of the cluster.
     *
     * @param request  The cluster state request
     * @param listener A listener to be notified with a result
     * @see Requests#clusterHealthRequest(String...)
     */
    void health(ClusterHealthRequest request, ActionListener<ClusterHealthResponse> listener);

    /**
     * The health of the cluster.
     */
    ClusterHealthRequestBuilder prepareHealth(String... indices);

    /**
     * The state of the cluster.
     *
     * @param request The cluster state request.
     * @return The result future
     * @see Requests#clusterStateRequest()
     */
    ActionFuture<ClusterStateResponse> state(ClusterStateRequest request);

    /**
     * The state of the cluster.
     *
     * @param request  The cluster state request.
     * @param listener A listener to be notified with a result
     * @see Requests#clusterStateRequest()
     */
    void state(ClusterStateRequest request, ActionListener<ClusterStateResponse> listener);

    /**
     * The state of the cluster.
     */
    ClusterStateRequestBuilder prepareState();

    /**
     * Updates settings in the cluster.
     */
    ActionFuture<ClusterUpdateSettingsResponse> updateSettings(ClusterUpdateSettingsRequest request);

    /**
     * Update settings in the cluster.
     */
    void updateSettings(ClusterUpdateSettingsRequest request, ActionListener<ClusterUpdateSettingsResponse> listener);

    /**
     * Update settings in the cluster.
     */
    ClusterUpdateSettingsRequestBuilder prepareUpdateSettings();

    /**
     * Re initialize each cluster node and pass them the secret store password.
     */
    NodesReloadSecureSettingsRequestBuilder prepareReloadSecureSettings();

    /**
     * Reroutes allocation of shards. Advance API.
     */
    ActionFuture<ClusterRerouteResponse> reroute(ClusterRerouteRequest request);

    /**
     * Reroutes allocation of shards. Advance API.
     */
    void reroute(ClusterRerouteRequest request, ActionListener<ClusterRerouteResponse> listener);

    /**
     * Update settings in the cluster.
     */
    ClusterRerouteRequestBuilder prepareReroute();

    /**
     * Nodes info of the cluster.
     *
     * @param request The nodes info request
     * @return The result future
     * @see org.renameme.client.Requests#nodesInfoRequest(String...)
     */
    ActionFuture<NodesInfoResponse> nodesInfo(NodesInfoRequest request);

    /**
     * Nodes info of the cluster.
     *
     * @param request  The nodes info request
     * @param listener A listener to be notified with a result
     * @see org.renameme.client.Requests#nodesInfoRequest(String...)
     */
    void nodesInfo(NodesInfoRequest request, ActionListener<NodesInfoResponse> listener);

    /**
     * Nodes info of the cluster.
     */
    NodesInfoRequestBuilder prepareNodesInfo(String... nodesIds);

    /**
     * Cluster wide aggregated stats.
     *
     * @param request The cluster stats request
     * @return The result future
     * @see org.renameme.client.Requests#clusterStatsRequest
     */
    ActionFuture<ClusterStatsResponse> clusterStats(ClusterStatsRequest request);

    /**
     * Cluster wide aggregated stats
     *
     * @param request  The cluster stats request
     * @param listener A listener to be notified with a result
     * @see org.renameme.client.Requests#clusterStatsRequest()
     */
    void clusterStats(ClusterStatsRequest request, ActionListener<ClusterStatsResponse> listener);

    ClusterStatsRequestBuilder prepareClusterStats();

    /**
     * Nodes stats of the cluster.
     *
     * @param request The nodes stats request
     * @return The result future
     * @see org.renameme.client.Requests#nodesStatsRequest(String...)
     */
    ActionFuture<NodesStatsResponse> nodesStats(NodesStatsRequest request);

    /**
     * Nodes stats of the cluster.
     *
     * @param request  The nodes info request
     * @param listener A listener to be notified with a result
     * @see org.renameme.client.Requests#nodesStatsRequest(String...)
     */
    void nodesStats(NodesStatsRequest request, ActionListener<NodesStatsResponse> listener);

    /**
     * Nodes stats of the cluster.
     */
    NodesStatsRequestBuilder prepareNodesStats(String... nodesIds);

    /**
     * Returns top N hot-threads samples per node. The hot-threads are only
     * sampled for the node ids specified in the request. Nodes usage of the
     * cluster.
     *
     * @param request
     *            The nodes usage request
     * @return The result future
     * @see org.renameme.client.Requests#nodesUsageRequest(String...)
     */
    ActionFuture<NodesUsageResponse> nodesUsage(NodesUsageRequest request);

    /**
     * Nodes usage of the cluster.
     *
     * @param request
     *            The nodes usage request
     * @param listener
     *            A listener to be notified with a result
     * @see org.renameme.client.Requests#nodesUsageRequest(String...)
     */
    void nodesUsage(NodesUsageRequest request, ActionListener<NodesUsageResponse> listener);

    /**
     * Nodes usage of the cluster.
     */
    NodesUsageRequestBuilder prepareNodesUsage(String... nodesIds);

    /**
     * Returns top N hot-threads samples per node. The hot-threads are only
     * sampled for the node ids specified in the request.
     *
     */
    ActionFuture<NodesHotThreadsResponse> nodesHotThreads(NodesHotThreadsRequest request);

    /**
     * Returns top N hot-threads samples per node. The hot-threads are only sampled
     * for the node ids specified in the request.
     */
    void nodesHotThreads(NodesHotThreadsRequest request, ActionListener<NodesHotThreadsResponse> listener);

    /**
     * Returns a request builder to fetch top N hot-threads samples per node. The hot-threads are only sampled
     * for the node ids provided. Note: Use {@code *} to fetch samples for all nodes
     */
    NodesHotThreadsRequestBuilder prepareNodesHotThreads(String... nodesIds);

    /**
     * List tasks
     *
     * @param request The nodes tasks request
     * @return The result future
     * @see org.renameme.client.Requests#listTasksRequest()
     */
    ActionFuture<ListTasksResponse> listTasks(ListTasksRequest request);

    /**
     * List active tasks
     *
     * @param request  The nodes tasks request
     * @param listener A listener to be notified with a result
     * @see org.renameme.client.Requests#listTasksRequest()
     */
    void listTasks(ListTasksRequest request, ActionListener<ListTasksResponse> listener);

    /**
     * List active tasks
     */
    ListTasksRequestBuilder prepareListTasks(String... nodesIds);

    /**
     * Get a task.
     *
     * @param request the request
     * @return the result future
     * @see org.renameme.client.Requests#getTaskRequest()
     */
    ActionFuture<GetTaskResponse> getTask(GetTaskRequest request);

    /**
     * Get a task.
     *
     * @param request the request
     * @param listener A listener to be notified with the result
     * @see org.renameme.client.Requests#getTaskRequest()
     */
    void getTask(GetTaskRequest request, ActionListener<GetTaskResponse> listener);

    /**
     * Fetch a task by id.
     */
    GetTaskRequestBuilder prepareGetTask(String taskId);

    /**
     * Fetch a task by id.
     */
    GetTaskRequestBuilder prepareGetTask(TaskId taskId);

    /**
     * Cancel tasks
     *
     * @param request The nodes tasks request
     * @return The result future
     * @see org.renameme.client.Requests#cancelTasksRequest()
     */
    ActionFuture<CancelTasksResponse> cancelTasks(CancelTasksRequest request);

    /**
     * Cancel active tasks
     *
     * @param request  The nodes tasks request
     * @param listener A listener to be notified with a result
     * @see org.renameme.client.Requests#cancelTasksRequest()
     */
    void cancelTasks(CancelTasksRequest request, ActionListener<CancelTasksResponse> listener);

    /**
     * Cancel active tasks
     */
    CancelTasksRequestBuilder prepareCancelTasks(String... nodesIds);

    /**
     * Returns list of shards the given search would be executed on.
     */
    ActionFuture<ClusterSearchShardsResponse> searchShards(ClusterSearchShardsRequest request);

    /**
     * Returns list of shards the given search would be executed on.
     */
    void searchShards(ClusterSearchShardsRequest request, ActionListener<ClusterSearchShardsResponse> listener);

    /**
     * Returns list of shards the given search would be executed on.
     */
    ClusterSearchShardsRequestBuilder prepareSearchShards();

    /**
     * Returns list of shards the given search would be executed on.
     */
    ClusterSearchShardsRequestBuilder prepareSearchShards(String... indices);

    /**
     * Registers a snapshot repository.
     */
    ActionFuture<AcknowledgedResponse> putRepository(PutRepositoryRequest request);

    /**
     * Registers a snapshot repository.
     */
    void putRepository(PutRepositoryRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Registers a snapshot repository.
     */
    PutRepositoryRequestBuilder preparePutRepository(String name);

    /**
     * Unregisters a repository.
     */
    ActionFuture<AcknowledgedResponse> deleteRepository(DeleteRepositoryRequest request);

    /**
     * Unregisters a repository.
     */
    void deleteRepository(DeleteRepositoryRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Unregisters a repository.
     */
    DeleteRepositoryRequestBuilder prepareDeleteRepository(String name);

    /**
     * Gets repositories.
     */
    ActionFuture<GetRepositoriesResponse> getRepositories(GetRepositoriesRequest request);

    /**
     * Gets repositories.
     */
    void getRepositories(GetRepositoriesRequest request, ActionListener<GetRepositoriesResponse> listener);

    /**
     * Gets repositories.
     */
    GetRepositoriesRequestBuilder prepareGetRepositories(String... name);

    /**
     * Cleans up repository.
     */
    CleanupRepositoryRequestBuilder prepareCleanupRepository(String repository);

    /**
     * Cleans up repository.
     */
    ActionFuture<CleanupRepositoryResponse> cleanupRepository(CleanupRepositoryRequest repository);

    /**
     * Cleans up repository.
     */
    void cleanupRepository(CleanupRepositoryRequest repository, ActionListener<CleanupRepositoryResponse> listener);

    /**
     * Verifies a repository.
     */
    ActionFuture<VerifyRepositoryResponse> verifyRepository(VerifyRepositoryRequest request);

    /**
     * Verifies a repository.
     */
    void verifyRepository(VerifyRepositoryRequest request, ActionListener<VerifyRepositoryResponse> listener);

    /**
     * Verifies a repository.
     */
    VerifyRepositoryRequestBuilder prepareVerifyRepository(String name);

    /**
     * Creates a new snapshot.
     */
    ActionFuture<CreateSnapshotResponse> createSnapshot(CreateSnapshotRequest request);

    /**
     * Creates a new snapshot.
     */
    void createSnapshot(CreateSnapshotRequest request, ActionListener<CreateSnapshotResponse> listener);

    /**
     * Creates a new snapshot.
     */
    CreateSnapshotRequestBuilder prepareCreateSnapshot(String repository, String name);

    /**
     * Clones a snapshot.
     */
    CloneSnapshotRequestBuilder prepareCloneSnapshot(String repository, String source, String target);

    /**
     * Clones a snapshot.
     */
    ActionFuture<AcknowledgedResponse> cloneSnapshot(CloneSnapshotRequest request);

    /**
     * Clones a snapshot.
     */
    void cloneSnapshot(CloneSnapshotRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Get snapshots.
     */
    ActionFuture<GetSnapshotsResponse> getSnapshots(GetSnapshotsRequest request);

    /**
     * Get snapshot.
     */
    void getSnapshots(GetSnapshotsRequest request, ActionListener<GetSnapshotsResponse> listener);

    /**
     * Get snapshot.
     */
    GetSnapshotsRequestBuilder prepareGetSnapshots(String repository);

    /**
     * Delete snapshot.
     */
    ActionFuture<AcknowledgedResponse> deleteSnapshot(DeleteSnapshotRequest request);

    /**
     * Delete snapshot.
     */
    void deleteSnapshot(DeleteSnapshotRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Delete snapshot.
     */
    DeleteSnapshotRequestBuilder prepareDeleteSnapshot(String repository, String... snapshot);

    /**
     * Restores a snapshot.
     */
    ActionFuture<RestoreSnapshotResponse> restoreSnapshot(RestoreSnapshotRequest request);

    /**
     * Restores a snapshot.
     */
    void restoreSnapshot(RestoreSnapshotRequest request, ActionListener<RestoreSnapshotResponse> listener);

    /**
     * Restores a snapshot.
     */
    RestoreSnapshotRequestBuilder prepareRestoreSnapshot(String repository, String snapshot);

    /**
     * Returns a list of the pending cluster tasks, that are scheduled to be executed. This includes operations
     * that update the cluster state (for example, a create index operation)
     */
    void pendingClusterTasks(PendingClusterTasksRequest request, ActionListener<PendingClusterTasksResponse> listener);

    /**
     * Returns a list of the pending cluster tasks, that are scheduled to be executed. This includes operations
     * that update the cluster state (for example, a create index operation)
     */
    ActionFuture<PendingClusterTasksResponse> pendingClusterTasks(PendingClusterTasksRequest request);

    /**
     * Returns a list of the pending cluster tasks, that are scheduled to be executed. This includes operations
     * that update the cluster state (for example, a create index operation)
     */
    PendingClusterTasksRequestBuilder preparePendingClusterTasks();

    /**
     * Get snapshot status.
     */
    ActionFuture<SnapshotsStatusResponse> snapshotsStatus(SnapshotsStatusRequest request);

    /**
     * Get snapshot status.
     */
    void snapshotsStatus(SnapshotsStatusRequest request, ActionListener<SnapshotsStatusResponse> listener);

    /**
     * Get snapshot status.
     */
    SnapshotsStatusRequestBuilder prepareSnapshotStatus(String repository);

    /**
     * Get snapshot status.
     */
    SnapshotsStatusRequestBuilder prepareSnapshotStatus();

    /**
     * Stores an ingest pipeline
     */
    void putPipeline(PutPipelineRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Stores an ingest pipeline
     */
    ActionFuture<AcknowledgedResponse> putPipeline(PutPipelineRequest request);

    /**
     * Stores an ingest pipeline
     */
    PutPipelineRequestBuilder preparePutPipeline(String id, BytesReference source, XContentType xContentType);

    /**
     * Deletes a stored ingest pipeline
     */
    void deletePipeline(DeletePipelineRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Deletes a stored ingest pipeline
     */
    ActionFuture<AcknowledgedResponse> deletePipeline(DeletePipelineRequest request);

    /**
     * Deletes a stored ingest pipeline
     */
    DeletePipelineRequestBuilder prepareDeletePipeline();

    /**
     * Deletes a stored ingest pipeline
     */
    DeletePipelineRequestBuilder prepareDeletePipeline(String id);

    /**
     * Returns a stored ingest pipeline
     */
    void getPipeline(GetPipelineRequest request, ActionListener<GetPipelineResponse> listener);

    /**
     * Returns a stored ingest pipeline
     */
    ActionFuture<GetPipelineResponse> getPipeline(GetPipelineRequest request);

    /**
     * Returns a stored ingest pipeline
     */
    GetPipelineRequestBuilder prepareGetPipeline(String... ids);

    /**
     * Simulates an ingest pipeline
     */
    void simulatePipeline(SimulatePipelineRequest request, ActionListener<SimulatePipelineResponse> listener);

    /**
     * Simulates an ingest pipeline
     */
    ActionFuture<SimulatePipelineResponse> simulatePipeline(SimulatePipelineRequest request);

    /**
     * Simulates an ingest pipeline
     */
    SimulatePipelineRequestBuilder prepareSimulatePipeline(BytesReference source, XContentType xContentType);

    /**
     * Explain the allocation of a shard
     */
    void allocationExplain(ClusterAllocationExplainRequest request, ActionListener<ClusterAllocationExplainResponse> listener);

    /**
     * Explain the allocation of a shard
     */
    ActionFuture<ClusterAllocationExplainResponse> allocationExplain(ClusterAllocationExplainRequest request);

    /**
     * Explain the allocation of a shard
     */
    ClusterAllocationExplainRequestBuilder prepareAllocationExplain();

    /**
     * Store a script in the cluster state
     */
    PutStoredScriptRequestBuilder preparePutStoredScript();

    /**
     * Delete a script from the cluster state
     */
    void deleteStoredScript(DeleteStoredScriptRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Delete a script from the cluster state
     */
    ActionFuture<AcknowledgedResponse> deleteStoredScript(DeleteStoredScriptRequest request);

    /**
     * Delete a script from the cluster state
     */
    DeleteStoredScriptRequestBuilder prepareDeleteStoredScript();

    /**
     * Delete a script from the cluster state
     */
    DeleteStoredScriptRequestBuilder prepareDeleteStoredScript(String id);

    /**
     * Store a script in the cluster state
     */
    void putStoredScript(PutStoredScriptRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Store a script in the cluster state
     */
    ActionFuture<AcknowledgedResponse> putStoredScript(PutStoredScriptRequest request);

    /**
     * Get a script from the cluster state
     */
    GetStoredScriptRequestBuilder prepareGetStoredScript();

    /**
     * Get a script from the cluster state
     */
    GetStoredScriptRequestBuilder prepareGetStoredScript(String id);

    /**
     * Get a script from the cluster state
     */
    void getStoredScript(GetStoredScriptRequest request, ActionListener<GetStoredScriptResponse> listener);

    /**
     * Get a script from the cluster state
     */
    ActionFuture<GetStoredScriptResponse> getStoredScript(GetStoredScriptRequest request);

    /**
     * List dangling indices on all nodes.
     */
    void listDanglingIndices(ListDanglingIndicesRequest request, ActionListener<ListDanglingIndicesResponse> listener);

    /**
     * List dangling indices on all nodes.
     */
    ActionFuture<ListDanglingIndicesResponse> listDanglingIndices(ListDanglingIndicesRequest request);

    /**
     * Restore specified dangling indices.
     */
    void importDanglingIndex(ImportDanglingIndexRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Restore specified dangling indices.
     */
    ActionFuture<AcknowledgedResponse> importDanglingIndex(ImportDanglingIndexRequest request);

    /**
     * Delete specified dangling indices.
     */
    void deleteDanglingIndex(DeleteDanglingIndexRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Delete specified dangling indices.
     */
    ActionFuture<AcknowledgedResponse> deleteDanglingIndex(DeleteDanglingIndexRequest request);
}
