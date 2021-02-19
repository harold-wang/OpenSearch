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

package org.renameme.action.admin.indices.get;

import org.renameme.action.ActionListener;
import org.renameme.action.IndicesRequest;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.replication.ClusterStateCreationUtils;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.settings.IndexScopedSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.settings.SettingsFilter;
import org.renameme.common.settings.SettingsModule;
import org.renameme.common.util.concurrent.ThreadContext;
import org.renameme.index.Index;
import org.renameme.indices.IndicesService;
import org.renameme.test.ESSingleNodeTestCase;
import org.renameme.test.transport.CapturingTransport;
import org.renameme.threadpool.TestThreadPool;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class GetIndexActionTests extends ESSingleNodeTestCase {

    private TransportService transportService;
    private ClusterService clusterService;
    private IndicesService indicesService;
    private ThreadPool threadPool;
    private SettingsFilter settingsFilter;
    private final String indexName = "test_index";

    private TestTransportGetIndexAction getIndexAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        settingsFilter = new SettingsModule(Settings.EMPTY, emptyList(), emptyList(), emptySet()).getSettingsFilter();
        threadPool = new TestThreadPool("GetIndexActionTests");
        clusterService = getInstanceFromNode(ClusterService.class);
        indicesService = getInstanceFromNode(IndicesService.class);
        CapturingTransport capturingTransport = new CapturingTransport();
        transportService = capturingTransport.createTransportService(clusterService.getSettings(), threadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> clusterService.localNode(), null, emptySet());
        transportService.start();
        transportService.acceptIncomingRequests();
        getIndexAction = new GetIndexActionTests.TestTransportGetIndexAction();
    }

    @After
    public void tearDown() throws Exception {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
        threadPool = null;
        super.tearDown();
    }

    public void testIncludeDefaults() {
        GetIndexRequest defaultsRequest = new GetIndexRequest().indices(indexName).includeDefaults(true);
        getIndexAction.execute(null, defaultsRequest, ActionListener.wrap(
            defaultsResponse -> assertNotNull(
                "index.refresh_interval should be set as we are including defaults",
                defaultsResponse.getSetting(indexName, "index.refresh_interval")
            ), exception -> {
                throw new AssertionError(exception);
            })
        );
    }

    public void testDoNotIncludeDefaults() {
        GetIndexRequest noDefaultsRequest = new GetIndexRequest().indices(indexName);
        getIndexAction.execute(null, noDefaultsRequest, ActionListener.wrap(
            noDefaultsResponse -> assertNull(
                "index.refresh_interval should be null as it was never set",
                noDefaultsResponse.getSetting(indexName, "index.refresh_interval")
            ), exception -> {
                throw new AssertionError(exception);
            })
        );
    }

    class TestTransportGetIndexAction extends TransportGetIndexAction {

        TestTransportGetIndexAction() {
            super(GetIndexActionTests.this.transportService, GetIndexActionTests.this.clusterService,
                GetIndexActionTests.this.threadPool, settingsFilter, new ActionFilters(emptySet()),
                new GetIndexActionTests.Resolver(), indicesService, IndexScopedSettings.DEFAULT_SCOPED_SETTINGS);
        }

        @Override
        protected void doMasterOperation(GetIndexRequest request, String[] concreteIndices, ClusterState state,
                                       ActionListener<GetIndexResponse> listener) {
            ClusterState stateWithIndex = ClusterStateCreationUtils.state(indexName, 1, 1);
            super.doMasterOperation(request, concreteIndices, stateWithIndex, listener);
        }
    }

    static class Resolver extends IndexNameExpressionResolver {
        Resolver() {
            super(new ThreadContext(Settings.EMPTY));
        }

        @Override
        public String[] concreteIndexNames(ClusterState state, IndicesRequest request) {
            return request.indices();
        }

        @Override
        public Index[] concreteIndices(ClusterState state, IndicesRequest request) {
            Index[] out = new Index[request.indices().length];
            for (int x = 0; x < out.length; x++) {
                out[x] = new Index(request.indices()[x], "_na_");
            }
            return out;
        }
    }
}
