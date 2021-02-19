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

package org.renameme.action;

import org.renameme.action.main.MainAction;
import org.renameme.action.main.TransportMainAction;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.TransportAction;
import org.renameme.client.node.NodeClient;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.node.DiscoveryNodes;
import org.renameme.common.settings.ClusterSettings;
import org.renameme.common.settings.IndexScopedSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.settings.SettingsFilter;
import org.renameme.common.settings.SettingsModule;
import org.renameme.common.util.concurrent.ThreadContext;
import org.renameme.plugins.ActionPlugin;
import org.renameme.plugins.ActionPlugin.ActionHandler;
import org.renameme.rest.RestChannel;
import org.renameme.rest.RestController;
import org.renameme.rest.RestHandler;
import org.renameme.rest.RestRequest;
import org.renameme.rest.RestRequest.Method;
import org.renameme.rest.action.RestMainAction;
import org.renameme.tasks.Task;
import org.renameme.tasks.TaskManager;
import org.renameme.test.ESTestCase;
import org.renameme.threadpool.TestThreadPool;
import org.renameme.threadpool.ThreadPool;
import org.renameme.usage.UsageService;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.startsWith;

public class ActionModuleTests extends ESTestCase {
    public void testSetupActionsContainsKnownBuiltin() {
        assertThat(ActionModule.setupActions(emptyList()),
                hasEntry(MainAction.INSTANCE.name(), new ActionHandler<>(MainAction.INSTANCE, TransportMainAction.class)));
    }

    public void testPluginCantOverwriteBuiltinAction() {
        ActionPlugin dupsMainAction = new ActionPlugin() {
            @Override
            public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
                return singletonList(new ActionHandler<>(MainAction.INSTANCE, TransportMainAction.class));
            }
        };
        Exception e = expectThrows(IllegalArgumentException.class, () -> ActionModule.setupActions(singletonList(dupsMainAction)));
        assertEquals("action for name [" + MainAction.NAME + "] already registered", e.getMessage());
    }

    public void testPluginCanRegisterAction() {
        class FakeRequest extends ActionRequest {
            @Override
            public ActionRequestValidationException validate() {
                return null;
            }
        }
        class FakeTransportAction extends TransportAction<FakeRequest, ActionResponse> {
            protected FakeTransportAction(String actionName, ActionFilters actionFilters, TaskManager taskManager) {
                super(actionName, actionFilters, taskManager);
            }

            @Override
            protected void doExecute(Task task, FakeRequest request, ActionListener<ActionResponse> listener) {
            }
        }
        class FakeAction extends ActionType<ActionResponse> {
            protected FakeAction() {
                super("fake", null);
            }
        }
        FakeAction action = new FakeAction();
        ActionPlugin registersFakeAction = new ActionPlugin() {
            @Override
            public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
                return singletonList(new ActionHandler<>(action, FakeTransportAction.class));
            }
        };
        assertThat(ActionModule.setupActions(singletonList(registersFakeAction)),
                hasEntry("fake", new ActionHandler<>(action, FakeTransportAction.class)));
    }

    public void testSetupRestHandlerContainsKnownBuiltin() {
        SettingsModule settings = new SettingsModule(Settings.EMPTY);
        UsageService usageService = new UsageService();
        ActionModule actionModule = new ActionModule(false, settings.getSettings(),
            new IndexNameExpressionResolver(new ThreadContext(Settings.EMPTY)), settings.getIndexScopedSettings(),
            settings.getClusterSettings(), settings.getSettingsFilter(), null, emptyList(), null,
            null, usageService, null);
        actionModule.initRestHandlers(null);
        // At this point the easiest way to confirm that a handler is loaded is to try to register another one on top of it and to fail
        Exception e = expectThrows(IllegalArgumentException.class, () ->
            actionModule.getRestController().registerHandler(new RestHandler() {
                @Override
                public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
                }

                @Override
                public List<Route> routes() {
                    return singletonList(new Route(Method.GET, "/"));
                }
            }));
        assertThat(e.getMessage(), startsWith("Cannot replace existing handler for [/] for method: GET"));
    }

    public void testPluginCantOverwriteBuiltinRestHandler() throws IOException {
        ActionPlugin dupsMainAction = new ActionPlugin() {
            @Override
            public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                    IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                    IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
                return singletonList(new RestMainAction() {

                    @Override
                    public String getName() {
                        return "duplicated_" + super.getName();
                    }

                });
            }
        };
        SettingsModule settings = new SettingsModule(Settings.EMPTY);
        ThreadPool threadPool = new TestThreadPool(getTestName());
        try {
            UsageService usageService = new UsageService();
            ActionModule actionModule = new ActionModule(false, settings.getSettings(),
                new IndexNameExpressionResolver(threadPool.getThreadContext()), settings.getIndexScopedSettings(),
                settings.getClusterSettings(), settings.getSettingsFilter(), threadPool, singletonList(dupsMainAction),
                null, null, usageService, null);
            Exception e = expectThrows(IllegalArgumentException.class, () -> actionModule.initRestHandlers(null));
            assertThat(e.getMessage(), startsWith("Cannot replace existing handler for [/] for method: GET"));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testPluginCanRegisterRestHandler() {
        class FakeHandler implements RestHandler {
            @Override
            public List<Route> routes() {
                return singletonList(new Route(Method.GET, "/_dummy"));
            }

            @Override
            public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
            }
        }
        ActionPlugin registersFakeHandler = new ActionPlugin() {
            @Override
            public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                    IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                    IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
                return singletonList(new FakeHandler());
            }
        };

        SettingsModule settings = new SettingsModule(Settings.EMPTY);
        ThreadPool threadPool = new TestThreadPool(getTestName());
        try {
            UsageService usageService = new UsageService();
            ActionModule actionModule = new ActionModule(false, settings.getSettings(),
                new IndexNameExpressionResolver(threadPool.getThreadContext()), settings.getIndexScopedSettings(),
                settings.getClusterSettings(), settings.getSettingsFilter(), threadPool, singletonList(registersFakeHandler),
                null, null, usageService, null);
            actionModule.initRestHandlers(null);
            // At this point the easiest way to confirm that a handler is loaded is to try to register another one on top of it and to fail
            Exception e = expectThrows(IllegalArgumentException.class, () ->
                actionModule.getRestController().registerHandler(new RestHandler() {
                    @Override
                    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
                    }

                    @Override
                    public List<Route> routes() {
                        return singletonList(new Route(Method.GET, "/_dummy"));
                    }
                }));
            assertThat(e.getMessage(), startsWith("Cannot replace existing handler for [/_dummy] for method: GET"));
        } finally {
            threadPool.shutdown();
        }
    }
}
