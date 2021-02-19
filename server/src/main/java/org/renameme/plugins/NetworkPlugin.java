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
package org.renameme.plugins;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.renameme.common.io.stream.NamedWriteableRegistry;
import org.renameme.common.network.NetworkService;
import org.renameme.common.settings.ClusterSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.util.BigArrays;
import org.renameme.common.util.PageCacheRecycler;
import org.renameme.common.util.concurrent.ThreadContext;
import org.renameme.common.xcontent.NamedXContentRegistry;
import org.renameme.http.HttpServerTransport;
import org.renameme.indices.breaker.CircuitBreakerService;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.Transport;
import org.renameme.transport.TransportInterceptor;

/**
 * Plugin for extending network and transport related classes
 */
public interface NetworkPlugin {

    /**
     * Returns a list of {@link TransportInterceptor} instances that are used to intercept incoming and outgoing
     * transport (inter-node) requests. This must not return <code>null</code>
     *
     * @param namedWriteableRegistry registry of all named writeables registered
     * @param threadContext a {@link ThreadContext} of the current nodes or clients {@link ThreadPool} that can be used to set additional
     *                      headers in the interceptors
     */
    default List<TransportInterceptor> getTransportInterceptors(NamedWriteableRegistry namedWriteableRegistry,
                                                                ThreadContext threadContext) {
        return Collections.emptyList();
    }

    /**
     * Returns a map of {@link Transport} suppliers.
     * See {@link org.elasticsearch.common.network.NetworkModule#TRANSPORT_TYPE_KEY} to configure a specific implementation.
     */
    default Map<String, Supplier<Transport>> getTransports(Settings settings, ThreadPool threadPool, PageCacheRecycler pageCacheRecycler,
                                                           CircuitBreakerService circuitBreakerService,
                                                           NamedWriteableRegistry namedWriteableRegistry, NetworkService networkService) {
        return Collections.emptyMap();
    }

    /**
     * Returns a map of {@link HttpServerTransport} suppliers.
     * See {@link org.elasticsearch.common.network.NetworkModule#HTTP_TYPE_SETTING} to configure a specific implementation.
     */
    default Map<String, Supplier<HttpServerTransport>> getHttpTransports(Settings settings, ThreadPool threadPool, BigArrays bigArrays,
                                                                         PageCacheRecycler pageCacheRecycler,
                                                                         CircuitBreakerService circuitBreakerService,
                                                                         NamedXContentRegistry xContentRegistry,
                                                                         NetworkService networkService,
                                                                         HttpServerTransport.Dispatcher dispatcher,
                                                                         ClusterSettings clusterSettings) {
        return Collections.emptyMap();
    }
}
