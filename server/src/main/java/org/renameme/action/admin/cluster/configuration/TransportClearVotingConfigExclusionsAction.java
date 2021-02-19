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
package org.renameme.action.admin.cluster.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.renameme.RenamemeException;
import org.renameme.RenamemeTimeoutException;
import org.renameme.action.ActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.TransportMasterNodeAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.ClusterStateObserver;
import org.renameme.cluster.ClusterStateObserver.Listener;
import org.renameme.cluster.ClusterStateUpdateTask;
import org.renameme.cluster.block.ClusterBlockException;
import org.renameme.cluster.block.ClusterBlockLevel;
import org.renameme.cluster.coordination.CoordinationMetadata;
import org.renameme.cluster.coordination.CoordinationMetadata.VotingConfigExclusion;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.metadata.Metadata;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.Priority;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.common.unit.TimeValue;
import org.renameme.threadpool.ThreadPool;
import org.renameme.threadpool.ThreadPool.Names;
import org.renameme.transport.TransportService;

import java.io.IOException;
import java.util.function.Predicate;

public class TransportClearVotingConfigExclusionsAction
    extends TransportMasterNodeAction<ClearVotingConfigExclusionsRequest, ClearVotingConfigExclusionsResponse> {

    private static final Logger logger = LogManager.getLogger(TransportClearVotingConfigExclusionsAction.class);

    @Inject
    public TransportClearVotingConfigExclusionsAction(TransportService transportService, ClusterService clusterService,
                                                      ThreadPool threadPool, ActionFilters actionFilters,
                                                      IndexNameExpressionResolver indexNameExpressionResolver) {
        super(ClearVotingConfigExclusionsAction.NAME, transportService, clusterService, threadPool, actionFilters,
            ClearVotingConfigExclusionsRequest::new, indexNameExpressionResolver);
    }

    @Override
    protected String executor() {
        return Names.SAME;
    }

    @Override
    protected ClearVotingConfigExclusionsResponse read(StreamInput in) throws IOException {
        return new ClearVotingConfigExclusionsResponse(in);
    }

    @Override
    protected void masterOperation(ClearVotingConfigExclusionsRequest request, ClusterState initialState,
                                   ActionListener<ClearVotingConfigExclusionsResponse> listener) throws Exception {

        final long startTimeMillis = threadPool.relativeTimeInMillis();

        final Predicate<ClusterState> allExclusionsRemoved = newState -> {
            for (VotingConfigExclusion tombstone : initialState.getVotingConfigExclusions()) {
                // NB checking for the existence of any node with this persistent ID, because persistent IDs are how votes are counted.
                if (newState.nodes().nodeExists(tombstone.getNodeId())) {
                    return false;
                }
            }
            return true;
        };

        if (request.getWaitForRemoval() && allExclusionsRemoved.test(initialState) == false) {
            final ClusterStateObserver clusterStateObserver = new ClusterStateObserver(initialState, clusterService, request.getTimeout(),
                logger, threadPool.getThreadContext());

            clusterStateObserver.waitForNextChange(new Listener() {
                @Override
                public void onNewClusterState(ClusterState state) {
                    submitClearVotingConfigExclusionsTask(request, startTimeMillis, listener);
                }

                @Override
                public void onClusterServiceClose() {
                    listener.onFailure(new RenamemeException("cluster service closed while waiting for removal of nodes "
                        + initialState.getVotingConfigExclusions()));
                }

                @Override
                public void onTimeout(TimeValue timeout) {
                    listener.onFailure(new RenamemeTimeoutException(
                        "timed out waiting for removal of nodes; if nodes should not be removed, set waitForRemoval to false. "
                        + initialState.getVotingConfigExclusions()));
                }
            }, allExclusionsRemoved);
        } else {
            submitClearVotingConfigExclusionsTask(request, startTimeMillis, listener);
        }
    }

    private void submitClearVotingConfigExclusionsTask(ClearVotingConfigExclusionsRequest request, long startTimeMillis,
                                                       ActionListener<ClearVotingConfigExclusionsResponse> listener) {
        clusterService.submitStateUpdateTask("clear-voting-config-exclusions", new ClusterStateUpdateTask(Priority.URGENT) {
            @Override
            public ClusterState execute(ClusterState currentState) {
                final CoordinationMetadata newCoordinationMetadata =
                        CoordinationMetadata.builder(currentState.coordinationMetadata()).clearVotingConfigExclusions().build();
                final Metadata newMetadata = Metadata.builder(currentState.metadata()).
                        coordinationMetadata(newCoordinationMetadata).build();
                return ClusterState.builder(currentState).metadata(newMetadata).build();
            }

            @Override
            public void onFailure(String source, Exception e) {
                listener.onFailure(e);
            }

            @Override
            public TimeValue timeout() {
                return TimeValue.timeValueMillis(request.getTimeout().millis() + startTimeMillis - threadPool.relativeTimeInMillis());
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                listener.onResponse(new ClearVotingConfigExclusionsResponse());
            }
        });
    }

    @Override
    protected ClusterBlockException checkBlock(ClearVotingConfigExclusionsRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
