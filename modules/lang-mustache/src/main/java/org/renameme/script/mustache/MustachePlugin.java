/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.renameme.script.mustache;

import org.renameme.action.ActionRequest;
import org.renameme.action.ActionResponse;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.node.DiscoveryNodes;
import org.renameme.common.settings.ClusterSettings;
import org.renameme.common.settings.IndexScopedSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.settings.SettingsFilter;
import org.renameme.plugins.ActionPlugin;
import org.renameme.plugins.Plugin;
import org.renameme.plugins.ScriptPlugin;
import org.renameme.plugins.SearchPlugin;
import org.renameme.rest.RestController;
import org.renameme.rest.RestHandler;
import org.renameme.script.ScriptContext;
import org.renameme.script.ScriptEngine;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class MustachePlugin extends Plugin implements ScriptPlugin, ActionPlugin, SearchPlugin {

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>>contexts) {
        return new MustacheScriptEngine();
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(new ActionHandler<>(SearchTemplateAction.INSTANCE, TransportSearchTemplateAction.class),
                new ActionHandler<>(MultiSearchTemplateAction.INSTANCE, TransportMultiSearchTemplateAction.class));
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
            IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter, IndexNameExpressionResolver indexNameExpressionResolver,
            Supplier<DiscoveryNodes> nodesInCluster) {
        return Arrays.asList(
                new RestSearchTemplateAction(),
                new RestMultiSearchTemplateAction(settings),
                new RestRenderSearchTemplateAction());
    }
}
