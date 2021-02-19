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

package org.renameme.plugin.repository.url;

import org.renameme.cluster.service.ClusterService;
import org.renameme.common.settings.Setting;
import org.renameme.common.xcontent.NamedXContentRegistry;
import org.renameme.env.Environment;
import org.renameme.indices.recovery.RecoverySettings;
import org.renameme.plugins.Plugin;
import org.renameme.plugins.RepositoryPlugin;
import org.renameme.repositories.Repository;
import org.renameme.repositories.url.URLRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class URLRepositoryPlugin extends Plugin implements RepositoryPlugin {

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(
            URLRepository.ALLOWED_URLS_SETTING,
            URLRepository.REPOSITORIES_URL_SETTING,
            URLRepository.SUPPORTED_PROTOCOLS_SETTING
        );
    }

    @Override
    public Map<String, Repository.Factory> getRepositories(Environment env, NamedXContentRegistry namedXContentRegistry,
                                                           ClusterService clusterService, RecoverySettings recoverySettings) {
        return Collections.singletonMap(URLRepository.TYPE,
            metadata -> new URLRepository(metadata, env, namedXContentRegistry, clusterService, recoverySettings));
    }
}
