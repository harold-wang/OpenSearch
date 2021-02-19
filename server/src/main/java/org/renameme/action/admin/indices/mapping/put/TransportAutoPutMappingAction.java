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
package org.renameme.action.admin.indices.mapping.put;

import org.renameme.action.ActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.AcknowledgedResponse;
import org.renameme.action.support.master.TransportMasterNodeAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.block.ClusterBlockException;
import org.renameme.cluster.block.ClusterBlockLevel;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.metadata.MetadataMappingService;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.index.Index;
import org.renameme.tasks.Task;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.io.IOException;

import static org.renameme.action.admin.indices.mapping.put.TransportPutMappingAction.performMappingUpdate;

public class TransportAutoPutMappingAction extends TransportMasterNodeAction<PutMappingRequest, AcknowledgedResponse> {

    private final MetadataMappingService metadataMappingService;

    @Inject
    public TransportAutoPutMappingAction(
            final TransportService transportService,
            final ClusterService clusterService,
            final ThreadPool threadPool,
            final MetadataMappingService metadataMappingService,
            final ActionFilters actionFilters,
            final IndexNameExpressionResolver indexNameExpressionResolver) {
        super(AutoPutMappingAction.NAME, transportService, clusterService, threadPool, actionFilters,
            PutMappingRequest::new, indexNameExpressionResolver);
        this.metadataMappingService = metadataMappingService;
    }

    @Override
    protected String executor() {
        // we go async right away
        return ThreadPool.Names.SAME;
    }

    @Override
    protected AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    protected void doExecute(Task task, PutMappingRequest request, ActionListener<AcknowledgedResponse> listener) {
        if (request.getConcreteIndex() == null) {
            throw new IllegalArgumentException("concrete index missing");
        }

        super.doExecute(task, request, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(PutMappingRequest request, ClusterState state) {
        String[] indices = new String[] {request.getConcreteIndex().getName()};
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, indices);
    }

    @Override
    protected void masterOperation(final PutMappingRequest request, final ClusterState state,
                                   final ActionListener<AcknowledgedResponse> listener) {
        final Index[] concreteIndices = new Index[] {request.getConcreteIndex()};
        performMappingUpdate(concreteIndices, request, listener, metadataMappingService);
    }

}
