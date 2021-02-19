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

package org.renameme.action.ingest;

import org.renameme.action.ActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.AcknowledgedResponse;
import org.renameme.action.support.master.TransportMasterNodeAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.block.ClusterBlockException;
import org.renameme.cluster.block.ClusterBlockLevel;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.ingest.IngestService;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.io.IOException;

public class DeletePipelineTransportAction extends TransportMasterNodeAction<DeletePipelineRequest, AcknowledgedResponse> {

    private final IngestService ingestService;

    @Inject
    public DeletePipelineTransportAction(ThreadPool threadPool, IngestService ingestService, TransportService transportService,
                                         ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(DeletePipelineAction.NAME, transportService, ingestService.getClusterService(),
            threadPool, actionFilters, DeletePipelineRequest::new, indexNameExpressionResolver);
        this.ingestService = ingestService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    protected void masterOperation(DeletePipelineRequest request, ClusterState state,
                                   ActionListener<AcknowledgedResponse> listener) throws Exception {
        ingestService.delete(request, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(DeletePipelineRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

}
