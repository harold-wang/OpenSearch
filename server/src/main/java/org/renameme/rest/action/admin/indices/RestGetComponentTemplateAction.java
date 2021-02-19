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

import org.renameme.action.admin.indices.template.get.GetComponentTemplateAction;
import org.renameme.client.node.NodeClient;
import org.renameme.common.settings.Settings;
import org.renameme.rest.BaseRestHandler;
import org.renameme.rest.RestRequest;
import org.renameme.rest.RestStatus;
import org.renameme.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.renameme.rest.RestRequest.Method.GET;
import static org.renameme.rest.RestRequest.Method.HEAD;
import static org.renameme.rest.RestStatus.NOT_FOUND;
import static org.renameme.rest.RestStatus.OK;

public class RestGetComponentTemplateAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return Arrays.asList(
            new Route(GET, "/_component_template"),
            new Route(GET, "/_component_template/{name}"),
            new Route(HEAD, "/_component_template/{name}"));
    }

    @Override
    public String getName() {
        return "get_component_template_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {

        final GetComponentTemplateAction.Request getRequest = new GetComponentTemplateAction.Request(request.param("name"));

        getRequest.local(request.paramAsBoolean("local", getRequest.local()));
        getRequest.masterNodeTimeout(request.paramAsTime("master_timeout", getRequest.masterNodeTimeout()));

        final boolean implicitAll = getRequest.name() == null;

        return channel ->
            client.execute(GetComponentTemplateAction.INSTANCE, getRequest,
                    new RestToXContentListener<GetComponentTemplateAction.Response>(channel) {
                        @Override
                        protected RestStatus getStatus(final GetComponentTemplateAction.Response response) {
                            final boolean templateExists = response.getComponentTemplates().isEmpty() == false;
                            return (templateExists || implicitAll) ? OK : NOT_FOUND;
                        }
                    });
    }

    @Override
    protected Set<String> responseParams() {
        return Settings.FORMAT_PARAMS;
    }

}
