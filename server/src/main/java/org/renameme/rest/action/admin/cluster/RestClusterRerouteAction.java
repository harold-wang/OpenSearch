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

package org.renameme.rest.action.admin.cluster;

import org.renameme.action.admin.cluster.reroute.ClusterRerouteRequest;
import org.renameme.client.Requests;
import org.renameme.client.node.NodeClient;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.routing.allocation.command.AllocationCommands;
import org.renameme.common.ParseField;
import org.renameme.common.Strings;
import org.renameme.common.settings.Settings;
import org.renameme.common.settings.SettingsFilter;
import org.renameme.common.xcontent.ObjectParser;
import org.renameme.common.xcontent.ObjectParser.ValueType;
import org.renameme.rest.BaseRestHandler;
import org.renameme.rest.RestRequest;
import org.renameme.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.renameme.rest.RestRequest.Method.POST;

public class RestClusterRerouteAction extends BaseRestHandler {
    private static final ObjectParser<ClusterRerouteRequest, Void> PARSER = new ObjectParser<>("cluster_reroute");
    static {
        PARSER.declareField((p, v, c) -> v.commands(AllocationCommands.fromXContent(p)), new ParseField("commands"),
                ValueType.OBJECT_ARRAY);
        PARSER.declareBoolean(ClusterRerouteRequest::dryRun, new ParseField("dry_run"));
    }

    private static final String DEFAULT_METRICS = Strings
            .arrayToCommaDelimitedString(EnumSet.complementOf(EnumSet.of(ClusterState.Metric.METADATA)).toArray());

    private final SettingsFilter settingsFilter;

    public RestClusterRerouteAction(SettingsFilter settingsFilter) {
        this.settingsFilter = settingsFilter;
    }

    @Override
    public List<Route> routes() {
        return singletonList(new Route(POST, "/_cluster/reroute"));
    }

    @Override
    public String getName() {
        return "cluster_reroute_action";
    }

    @Override
    public boolean allowSystemIndexAccessByDefault() {
        return true;
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        ClusterRerouteRequest clusterRerouteRequest = createRequest(request);
        settingsFilter.addFilterSettingParams(request);
        if (clusterRerouteRequest.explain()) {
            request.params().put("explain", Boolean.TRUE.toString());
        }
        // by default, return everything but metadata
        final String metric = request.param("metric");
        if (metric == null) {
            request.params().put("metric", DEFAULT_METRICS);
        }
        return channel -> client.admin().cluster().reroute(clusterRerouteRequest, new RestToXContentListener<>(channel));
    }

    private static final Set<String> RESPONSE_PARAMS;

    static {
        final Set<String> responseParams = new HashSet<>();
        responseParams.add("metric");
        responseParams.addAll(Settings.FORMAT_PARAMS);
        RESPONSE_PARAMS = Collections.unmodifiableSet(responseParams);
    }

    @Override
    protected Set<String> responseParams() {
        return RESPONSE_PARAMS;
    }

    public static ClusterRerouteRequest createRequest(RestRequest request) throws IOException {
        ClusterRerouteRequest clusterRerouteRequest = Requests.clusterRerouteRequest();
        clusterRerouteRequest.dryRun(request.paramAsBoolean("dry_run", clusterRerouteRequest.dryRun()));
        clusterRerouteRequest.explain(request.paramAsBoolean("explain", clusterRerouteRequest.explain()));
        clusterRerouteRequest.timeout(request.paramAsTime("timeout", clusterRerouteRequest.timeout()));
        clusterRerouteRequest.setRetryFailed(request.paramAsBoolean("retry_failed", clusterRerouteRequest.isRetryFailed()));
        clusterRerouteRequest.masterNodeTimeout(request.paramAsTime("master_timeout", clusterRerouteRequest.masterNodeTimeout()));
        request.applyContentParser(parser -> PARSER.parse(parser, clusterRerouteRequest, null));
        return clusterRerouteRequest;
    }
}
