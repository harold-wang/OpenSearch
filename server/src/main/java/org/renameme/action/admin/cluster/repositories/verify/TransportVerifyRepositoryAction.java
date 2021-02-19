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

package org.renameme.action.admin.cluster.repositories.verify;

import org.renameme.action.ActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.TransportMasterNodeAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.block.ClusterBlockException;
import org.renameme.cluster.block.ClusterBlockLevel;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.node.DiscoveryNode;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.repositories.RepositoriesService;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for verifying repository operation
 */
public class TransportVerifyRepositoryAction extends
    TransportMasterNodeAction<VerifyRepositoryRequest, VerifyRepositoryResponse> {

    private final RepositoriesService repositoriesService;


    @Inject
    public TransportVerifyRepositoryAction(TransportService transportService, ClusterService clusterService,
                                           RepositoriesService repositoriesService, ThreadPool threadPool, ActionFilters actionFilters,
                                           IndexNameExpressionResolver indexNameExpressionResolver) {
        super(VerifyRepositoryAction.NAME, transportService, clusterService, threadPool, actionFilters,
              VerifyRepositoryRequest::new, indexNameExpressionResolver);
        this.repositoriesService = repositoriesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected VerifyRepositoryResponse read(StreamInput in) throws IOException {
        return new VerifyRepositoryResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(VerifyRepositoryRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected void masterOperation(final VerifyRepositoryRequest request, ClusterState state,
                                   final ActionListener<VerifyRepositoryResponse> listener) {
        repositoriesService.verifyRepository(request.name(), ActionListener.delegateFailure(listener,
            (delegatedListener, verifyResponse) ->
                delegatedListener.onResponse(new VerifyRepositoryResponse(verifyResponse.toArray(new DiscoveryNode[0])))));
    }
}
