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

package org.renameme.action.admin.indices.settings.get;

import org.renameme.action.ActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.TransportMasterNodeReadAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.block.ClusterBlockException;
import org.renameme.cluster.block.ClusterBlockLevel;
import org.renameme.cluster.metadata.IndexMetadata;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.collect.ImmutableOpenMap;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.common.regex.Regex;
import org.renameme.common.settings.IndexScopedSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.settings.SettingsFilter;
import org.renameme.common.util.CollectionUtils;
import org.renameme.index.Index;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.io.IOException;


public class TransportGetSettingsAction extends TransportMasterNodeReadAction<GetSettingsRequest, GetSettingsResponse> {

    private final SettingsFilter settingsFilter;
    private final IndexScopedSettings indexScopedSettings;


    @Inject
    public TransportGetSettingsAction(TransportService transportService, ClusterService clusterService,
                                      ThreadPool threadPool, SettingsFilter settingsFilter, ActionFilters actionFilters,
                                      IndexNameExpressionResolver indexNameExpressionResolver, IndexScopedSettings indexedScopedSettings) {
        super(GetSettingsAction.NAME, transportService, clusterService, threadPool, actionFilters, GetSettingsRequest::new,
            indexNameExpressionResolver);
        this.settingsFilter = settingsFilter;
        this.indexScopedSettings = indexedScopedSettings;
    }

    @Override
    protected String executor() {
        // Very lightweight operation
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterBlockException checkBlock(GetSettingsRequest request, ClusterState state) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ,
            indexNameExpressionResolver.concreteIndexNames(state, request));
    }


    @Override
    protected GetSettingsResponse read(StreamInput in) throws IOException {
        return new GetSettingsResponse(in);
    }

    private static boolean isFilteredRequest(GetSettingsRequest request) {
        return CollectionUtils.isEmpty(request.names()) == false;
    }

    @Override
    protected void masterOperation(GetSettingsRequest request, ClusterState state, ActionListener<GetSettingsResponse> listener) {
        Index[] concreteIndices = indexNameExpressionResolver.concreteIndices(state, request);
        ImmutableOpenMap.Builder<String, Settings> indexToSettingsBuilder = ImmutableOpenMap.builder();
        ImmutableOpenMap.Builder<String, Settings> indexToDefaultSettingsBuilder = ImmutableOpenMap.builder();
        for (Index concreteIndex : concreteIndices) {
            IndexMetadata indexMetadata = state.getMetadata().index(concreteIndex);
            if (indexMetadata == null) {
                continue;
            }

            Settings indexSettings = settingsFilter.filter(indexMetadata.getSettings());
            if (request.humanReadable()) {
                indexSettings = IndexMetadata.addHumanReadableSettings(indexSettings);
            }

            if (isFilteredRequest(request)) {
                indexSettings = indexSettings.filter(k -> Regex.simpleMatch(request.names(), k));
            }

            indexToSettingsBuilder.put(concreteIndex.getName(), indexSettings);
            if (request.includeDefaults()) {
                Settings defaultSettings = settingsFilter.filter(indexScopedSettings.diff(indexSettings, Settings.EMPTY));
                if (isFilteredRequest(request)) {
                    defaultSettings = defaultSettings.filter(k -> Regex.simpleMatch(request.names(), k));
                }
                indexToDefaultSettingsBuilder.put(concreteIndex.getName(), defaultSettings);
            }
        }
        listener.onResponse(new GetSettingsResponse(indexToSettingsBuilder.build(), indexToDefaultSettingsBuilder.build()));
    }
}
