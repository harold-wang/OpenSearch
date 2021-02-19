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
import org.renameme.action.admin.indices.get.GetIndexRequest.Feature;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.master.info.TransportClusterInfoAction;
import org.renameme.cluster.ClusterState;
import org.renameme.cluster.metadata.AliasMetadata;
import org.renameme.cluster.metadata.IndexMetadata;
import org.renameme.cluster.metadata.IndexNameExpressionResolver;
import org.renameme.cluster.metadata.MappingMetadata;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.collect.ImmutableOpenMap;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.StreamInput;
import org.renameme.common.settings.IndexScopedSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.settings.SettingsFilter;
import org.renameme.indices.IndicesService;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Get index action.
 */
public class TransportGetIndexAction extends TransportClusterInfoAction<GetIndexRequest, GetIndexResponse> {

    private final IndicesService indicesService;
    private final IndexScopedSettings indexScopedSettings;
    private final SettingsFilter settingsFilter;

    @Inject
    public TransportGetIndexAction(TransportService transportService, ClusterService clusterService,
                                   ThreadPool threadPool, SettingsFilter settingsFilter, ActionFilters actionFilters,
                                   IndexNameExpressionResolver indexNameExpressionResolver, IndicesService indicesService,
                                   IndexScopedSettings indexScopedSettings) {
        super(GetIndexAction.NAME, transportService, clusterService, threadPool, actionFilters, GetIndexRequest::new,
                indexNameExpressionResolver);
        this.indicesService = indicesService;
        this.settingsFilter = settingsFilter;
        this.indexScopedSettings = indexScopedSettings;
    }

    @Override
    protected GetIndexResponse read(StreamInput in) throws IOException {
        return new GetIndexResponse(in);
    }

    @Override
    protected void doMasterOperation(final GetIndexRequest request, String[] concreteIndices, final ClusterState state,
                                     final ActionListener<GetIndexResponse> listener) {
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetadata>> mappingsResult = ImmutableOpenMap.of();
        ImmutableOpenMap<String, List<AliasMetadata>> aliasesResult = ImmutableOpenMap.of();
        ImmutableOpenMap<String, Settings> settings = ImmutableOpenMap.of();
        ImmutableOpenMap<String, Settings> defaultSettings = ImmutableOpenMap.of();
        ImmutableOpenMap<String, String> dataStreams = ImmutableOpenMap.<String, String>builder()
            .putAll(StreamSupport.stream(state.metadata().findDataStreams(concreteIndices).spliterator(), false)
                .collect(Collectors.toMap(k -> k.key, v -> v.value.getName()))).build();
        Feature[] features = request.features();
        boolean doneAliases = false;
        boolean doneMappings = false;
        boolean doneSettings = false;
        for (Feature feature : features) {
            switch (feature) {
            case MAPPINGS:
                    if (!doneMappings) {
                        try {
                            mappingsResult = state.metadata().findMappings(concreteIndices, request.types(),
                                    indicesService.getFieldFilter());
                            doneMappings = true;
                        } catch (IOException e) {
                            listener.onFailure(e);
                            return;
                        }
                    }
                    break;
            case ALIASES:
                    if (!doneAliases) {
                        aliasesResult = state.metadata().findAllAliases(concreteIndices);
                        doneAliases = true;
                    }
                    break;
            case SETTINGS:
                    if (!doneSettings) {
                        ImmutableOpenMap.Builder<String, Settings> settingsMapBuilder = ImmutableOpenMap.builder();
                        ImmutableOpenMap.Builder<String, Settings> defaultSettingsMapBuilder = ImmutableOpenMap.builder();
                        for (String index : concreteIndices) {
                            Settings indexSettings = state.metadata().index(index).getSettings();
                            if (request.humanReadable()) {
                                indexSettings = IndexMetadata.addHumanReadableSettings(indexSettings);
                            }
                            settingsMapBuilder.put(index, indexSettings);
                            if (request.includeDefaults()) {
                                Settings defaultIndexSettings =
                                    settingsFilter.filter(indexScopedSettings.diff(indexSettings, Settings.EMPTY));
                                defaultSettingsMapBuilder.put(index, defaultIndexSettings);
                            }
                        }
                        settings = settingsMapBuilder.build();
                        defaultSettings = defaultSettingsMapBuilder.build();
                        doneSettings = true;
                    }
                    break;

                default:
                    throw new IllegalStateException("feature [" + feature + "] is not valid");
            }
        }
        listener.onResponse(
            new GetIndexResponse(concreteIndices, mappingsResult, aliasesResult, settings, defaultSettings, dataStreams)
        );
    }
}
