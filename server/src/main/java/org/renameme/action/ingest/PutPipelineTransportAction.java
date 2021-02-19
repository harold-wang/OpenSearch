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
import org.renameme.action.admin.cluster.node.info.NodeInfo;
import org.renameme.action.admin.cluster.node.info.NodesInfoRequest;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.AcknowledgedResponse;
import org.renameme.action.support.master.TransportMasterNodeAction;
import org.renameme.client.OriginSettingClient;
import org.renameme.client.node.NodeClient;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.block.ClusterBlockException;
import org.renameme.cluster.block.ClusterBlockLevel;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.node.DiscoveryNode;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.ingest.IngestInfo;
import org.renameme.ingest.IngestService;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.renameme.ingest.IngestService.INGEST_ORIGIN;

public class PutPipelineTransportAction extends TransportMasterNodeAction<PutPipelineRequest, AcknowledgedResponse> {

    private final IngestService ingestService;
    private final OriginSettingClient client;

    @Inject
    public PutPipelineTransportAction(ThreadPool threadPool, TransportService transportService,
        ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
        IngestService ingestService, NodeClient client) {
        super(
            PutPipelineAction.NAME, transportService, ingestService.getClusterService(),
            threadPool, actionFilters, PutPipelineRequest::new, indexNameExpressionResolver
        );
        // This client is only used to perform an internal implementation detail,
        // so uses an internal origin context rather than the user context
        this.client = new OriginSettingClient(client, INGEST_ORIGIN);
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
    protected void masterOperation(PutPipelineRequest request, ClusterState state, ActionListener<AcknowledgedResponse> listener)
            throws Exception {
        NodesInfoRequest nodesInfoRequest = new NodesInfoRequest();
        nodesInfoRequest.clear()
            .addMetric(NodesInfoRequest.Metric.INGEST.metricName());
        client.admin().cluster().nodesInfo(nodesInfoRequest, ActionListener.wrap(nodeInfos -> {
            Map<DiscoveryNode, IngestInfo> ingestInfos = new HashMap<>();
            for (NodeInfo nodeInfo : nodeInfos.getNodes()) {
                ingestInfos.put(nodeInfo.getNode(), nodeInfo.getInfo(IngestInfo.class));
            }
            ingestService.putPipeline(ingestInfos, request, listener);
        }, listener::onFailure));
    }

    @Override
    protected ClusterBlockException checkBlock(PutPipelineRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

}
