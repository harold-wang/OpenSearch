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

package org.renameme.action.update;

import org.renameme.ResourceAlreadyExistsException;
import org.renameme.action.ActionListener;
import org.renameme.action.ActionRunnable;
import org.renameme.action.DocWriteRequest;
import org.renameme.action.RoutingMissingException;
import org.renameme.action.admin.indices.create.CreateIndexRequest;
import org.renameme.action.admin.indices.create.CreateIndexResponse;
import org.renameme.action.delete.DeleteRequest;
import org.renameme.action.delete.DeleteResponse;
import org.renameme.action.index.IndexRequest;
import org.renameme.action.index.IndexResponse;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.AutoCreateIndex;
import org.renameme.action.support.TransportActions;
import org.renameme.action.support.single.instance.TransportInstanceSingleOperationAction;
import org.renameme.client.node.NodeClient;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.metadata.Metadata;
import org.renameme.cluster.routing.PlainShardIterator;
import org.renameme.cluster.routing.ShardIterator;
import org.renameme.cluster.routing.ShardRouting;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.bytes.BytesReference;
import org.renameme.common.collect.Tuple;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.NotSerializableExceptionWrapper;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.common.xcontent.XContentHelper;
import org.renameme.common.xcontent.XContentType;
import org.renameme.index.IndexNotFoundException;
import org.renameme.index.IndexService;
import org.renameme.index.engine.VersionConflictEngineException;
import org.renameme.index.shard.IndexShard;
import org.renameme.index.shard.ShardId;
import org.renameme.indices.IndicesService;
import org.renameme.tasks.Task;
import org.renameme.threadpool.ThreadPool;
import org.renameme.threadpool.ThreadPool.Names;
import org.renameme.transport.TransportService;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.renameme.ExceptionsHelper.unwrapCause;
import static org.renameme.action.bulk.TransportSingleItemBulkWriteAction.toSingleItemBulkRequest;
import static org.renameme.action.bulk.TransportSingleItemBulkWriteAction.wrapBulkResponse;

public class TransportUpdateAction extends TransportInstanceSingleOperationAction<UpdateRequest, UpdateResponse> {

    private final AutoCreateIndex autoCreateIndex;
    private final UpdateHelper updateHelper;
    private final IndicesService indicesService;
    private final NodeClient client;
    private final ClusterService clusterService;

    @Inject
    public TransportUpdateAction(ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                 UpdateHelper updateHelper, ActionFilters actionFilters,
                                 IndexNameExpressionResolver indexNameExpressionResolver, IndicesService indicesService,
                                 AutoCreateIndex autoCreateIndex, NodeClient client) {
        super(UpdateAction.NAME, threadPool, clusterService, transportService, actionFilters,
            indexNameExpressionResolver, UpdateRequest::new);
        this.updateHelper = updateHelper;
        this.indicesService = indicesService;
        this.autoCreateIndex = autoCreateIndex;
        this.client = client;
        this.clusterService = clusterService;
    }

    @Override
    protected String executor(ShardId shardId) {
        final IndexService indexService = indicesService.indexServiceSafe(shardId.getIndex());
        return indexService.getIndexSettings().getIndexMetadata().isSystem() ? Names.SYSTEM_WRITE : Names.WRITE;
    }

    @Override
    protected UpdateResponse newResponse(StreamInput in) throws IOException {
        return new UpdateResponse(in);
    }

    @Override
    protected boolean retryOnFailure(Exception e) {
        return TransportActions.isShardNotAvailableException(e);
    }

    @Override
    protected void resolveRequest(ClusterState state, UpdateRequest request) {
        resolveAndValidateRouting(state.metadata(), request.concreteIndex(), request);
    }

    public static void resolveAndValidateRouting(Metadata metadata, String concreteIndex, UpdateRequest request) {
        request.routing((metadata.resolveWriteIndexRouting(request.routing(), request.index())));
        // Fail fast on the node that received the request, rather than failing when translating on the index or delete request.
        if (request.routing() == null && metadata.routingRequired(concreteIndex)) {
            throw new RoutingMissingException(concreteIndex, request.type(), request.id());
        }
    }

    @Override
    protected void doExecute(Task task, final UpdateRequest request, final ActionListener<UpdateResponse> listener) {
        if (request.isRequireAlias() && (clusterService.state().getMetadata().hasAlias(request.index()) == false)) {
            throw new IndexNotFoundException("["
                + DocWriteRequest.REQUIRE_ALIAS
                + "] request flag is [true] and ["
                + request.index()
                + "] is not an alias", request.index());
        }
        // if we don't have a master, we don't have metadata, that's fine, let it find a master using create index API
        if (autoCreateIndex.shouldAutoCreate(request.index(), clusterService.state())) {
            client.admin().indices().create(new CreateIndexRequest()
                .index(request.index())
                .cause("auto(update api)")
                .masterNodeTimeout(request.timeout()), new ActionListener<CreateIndexResponse>() {
                @Override
                public void onResponse(CreateIndexResponse result) {
                    innerExecute(task, request, listener);
                }

                @Override
                public void onFailure(Exception e) {
                    if (unwrapCause(e) instanceof ResourceAlreadyExistsException) {
                        // we have the index, do it
                        try {
                            innerExecute(task, request, listener);
                        } catch (Exception inner) {
                            inner.addSuppressed(e);
                            listener.onFailure(inner);
                        }
                    } else {
                        listener.onFailure(e);
                    }
                }
            });
        } else {
            innerExecute(task, request, listener);
        }
    }

    private void innerExecute(final Task task, final UpdateRequest request, final ActionListener<UpdateResponse> listener) {
        super.doExecute(task, request, listener);
    }

    @Override
    protected ShardIterator shards(ClusterState clusterState, UpdateRequest request) {
        if (request.getShardId() != null) {
            return clusterState.routingTable().index(request.concreteIndex()).shard(request.getShardId().getId()).primaryShardIt();
        }
        ShardIterator shardIterator = clusterService.operationRouting()
                .indexShards(clusterState, request.concreteIndex(), request.id(), request.routing());
        ShardRouting shard;
        while ((shard = shardIterator.nextOrNull()) != null) {
            if (shard.primary()) {
                return new PlainShardIterator(shardIterator.shardId(), Collections.singletonList(shard));
            }
        }
        return new PlainShardIterator(shardIterator.shardId(), Collections.emptyList());
    }

    @Override
    protected void shardOperation(final UpdateRequest request, final ActionListener<UpdateResponse> listener) {
        shardOperation(request, listener, 0);
    }

    protected void shardOperation(final UpdateRequest request, final ActionListener<UpdateResponse> listener, final int retryCount) {
        final ShardId shardId = request.getShardId();
        final IndexService indexService = indicesService.indexServiceSafe(shardId.getIndex());
        final IndexShard indexShard = indexService.getShard(shardId.getId());
        final UpdateHelper.Result result = updateHelper.prepare(request, indexShard, threadPool::absoluteTimeInMillis);
        switch (result.getResponseResult()) {
            case CREATED:
                IndexRequest upsertRequest = result.action();
                // we fetch it from the index request so we don't generate the bytes twice, its already done in the index request
                final BytesReference upsertSourceBytes = upsertRequest.source();
                client.bulk(toSingleItemBulkRequest(upsertRequest), wrapBulkResponse(
                        ActionListener.<IndexResponse>wrap(response -> {
                            UpdateResponse update = new UpdateResponse(response.getShardInfo(), response.getShardId(),
                                response.getType(), response.getId(), response.getSeqNo(), response.getPrimaryTerm(),
                                response.getVersion(), response.getResult());
                            if (request.fetchSource() != null && request.fetchSource().fetchSource()) {
                                Tuple<XContentType, Map<String, Object>> sourceAndContent =
                                        XContentHelper.convertToMap(upsertSourceBytes, true, upsertRequest.getContentType());
                                update.setGetResult(UpdateHelper.extractGetResult(request, request.concreteIndex(),
                                    response.getSeqNo(), response.getPrimaryTerm(), response.getVersion(), sourceAndContent.v2(),
                                    sourceAndContent.v1(), upsertSourceBytes));
                            } else {
                                update.setGetResult(null);
                            }
                            update.setForcedRefresh(response.forcedRefresh());
                            listener.onResponse(update);
                        }, exception -> handleUpdateFailureWithRetry(listener, request, exception, retryCount)))
                );

                break;
            case UPDATED:
                IndexRequest indexRequest = result.action();
                // we fetch it from the index request so we don't generate the bytes twice, its already done in the index request
                final BytesReference indexSourceBytes = indexRequest.source();
                client.bulk(toSingleItemBulkRequest(indexRequest), wrapBulkResponse(
                        ActionListener.<IndexResponse>wrap(response -> {
                            UpdateResponse update = new UpdateResponse(response.getShardInfo(), response.getShardId(),
                                response.getType(), response.getId(), response.getSeqNo(), response.getPrimaryTerm(),
                                response.getVersion(), response.getResult());
                            update.setGetResult(UpdateHelper.extractGetResult(request, request.concreteIndex(),
                                response.getSeqNo(), response.getPrimaryTerm(), response.getVersion(),
                                result.updatedSourceAsMap(), result.updateSourceContentType(), indexSourceBytes));
                            update.setForcedRefresh(response.forcedRefresh());
                            listener.onResponse(update);
                        }, exception -> handleUpdateFailureWithRetry(listener, request, exception, retryCount)))
                );
                break;
            case DELETED:
                DeleteRequest deleteRequest = result.action();
                client.bulk(toSingleItemBulkRequest(deleteRequest), wrapBulkResponse(
                        ActionListener.<DeleteResponse>wrap(response -> {
                            UpdateResponse update = new UpdateResponse(response.getShardInfo(), response.getShardId(), response.getType(),
                                response.getId(), response.getSeqNo(), response.getPrimaryTerm(), response.getVersion(),
                                response.getResult());
                            update.setGetResult(UpdateHelper.extractGetResult(request, request.concreteIndex(),
                                response.getSeqNo(), response.getPrimaryTerm(), response.getVersion(),
                                result.updatedSourceAsMap(), result.updateSourceContentType(), null));
                            update.setForcedRefresh(response.forcedRefresh());
                            listener.onResponse(update);
                        }, exception -> handleUpdateFailureWithRetry(listener, request, exception, retryCount)))
                );
                break;
            case NOOP:
                UpdateResponse update = result.action();
                IndexService indexServiceOrNull = indicesService.indexService(shardId.getIndex());
                if (indexServiceOrNull !=  null) {
                    IndexShard shard = indexService.getShardOrNull(shardId.getId());
                    if (shard != null) {
                        shard.noopUpdate(request.type());
                    }
                }
                listener.onResponse(update);
                break;
            default:
                throw new IllegalStateException("Illegal result " + result.getResponseResult());
        }
    }

    private void handleUpdateFailureWithRetry(final ActionListener<UpdateResponse> listener, final UpdateRequest request,
                                              final Exception failure, int retryCount) {
        final Throwable cause = unwrapCause(failure);
        if (cause instanceof VersionConflictEngineException) {
            if (retryCount < request.retryOnConflict()) {
                logger.trace("Retry attempt [{}] of [{}] on version conflict on [{}][{}][{}]",
                        retryCount + 1, request.retryOnConflict(), request.index(), request.getShardId(), request.id());
                threadPool.executor(executor(request.getShardId()))
                    .execute(ActionRunnable.wrap(listener, l -> shardOperation(request, l, retryCount + 1)));
                return;
            }
        }
        listener.onFailure(cause instanceof Exception ? (Exception) cause : new NotSerializableExceptionWrapper(cause));
    }
}
