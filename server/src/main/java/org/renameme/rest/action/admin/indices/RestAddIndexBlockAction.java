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

package org.renameme.rest.action.admin.indices;

import org.renameme.action.admin.indices.readonly.AddIndexBlockRequest;
import org.renameme.action.support.IndicesOptions;
import org.renameme.client.node.NodeClient;
import org.renameme.cluster.metadata.IndexMetadata;
import org.renameme.common.Strings;
import org.renameme.rest.BaseRestHandler;
import org.renameme.rest.RestRequest;
import org.renameme.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.renameme.rest.RestRequest.Method.PUT;

public class RestAddIndexBlockAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return Collections.singletonList(
            new Route(PUT, "/{index}/_block/{block}"));
    }

    @Override
    public String getName() {
        return "add_index_block_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        AddIndexBlockRequest addIndexBlockRequest = new AddIndexBlockRequest(
            IndexMetadata.APIBlock.fromName(request.param("block")),
            Strings.splitStringByCommaToArray(request.param("index")));
        addIndexBlockRequest.masterNodeTimeout(request.paramAsTime("master_timeout", addIndexBlockRequest.masterNodeTimeout()));
        addIndexBlockRequest.timeout(request.paramAsTime("timeout", addIndexBlockRequest.timeout()));
        addIndexBlockRequest.indicesOptions(IndicesOptions.fromRequest(request, addIndexBlockRequest.indicesOptions()));
        return channel -> client.admin().indices().addBlock(addIndexBlockRequest, new RestToXContentListener<>(channel));
    }

}
