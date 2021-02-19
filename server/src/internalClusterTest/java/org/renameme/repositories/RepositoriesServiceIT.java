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

package org.renameme.repositories;

import org.renameme.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.renameme.client.Client;
import org.renameme.cluster.metadata.RepositoryMetadata;
import org.renameme.common.settings.Settings;
import org.renameme.plugins.Plugin;
import org.renameme.repositories.fs.FsRepository;
import org.renameme.snapshots.mockstore.MockRepository;
import org.renameme.test.ESIntegTestCase;
import org.renameme.test.InternalTestCluster;

import java.util.Collection;
import java.util.Collections;

import static org.renameme.test.hamcrest.RenamemeAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

public class RepositoriesServiceIT extends ESIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(MockRepository.Plugin.class);
    }

    public void testUpdateRepository() {
        final InternalTestCluster cluster = internalCluster();

        final String repositoryName = "test-repo";

        final Client client = client();
        final RepositoriesService repositoriesService =
            cluster.getDataOrMasterNodeInstances(RepositoriesService.class).iterator().next();

        final Settings.Builder repoSettings = Settings.builder().put("location", randomRepoPath());

        assertAcked(client.admin().cluster().preparePutRepository(repositoryName)
                .setType(FsRepository.TYPE)
                .setSettings(repoSettings)
                .get());

        final GetRepositoriesResponse originalGetRepositoriesResponse =
            client.admin().cluster().prepareGetRepositories(repositoryName).get();

        assertThat(originalGetRepositoriesResponse.repositories(), hasSize(1));
        RepositoryMetadata originalRepositoryMetadata = originalGetRepositoriesResponse.repositories().get(0);

        assertThat(originalRepositoryMetadata.type(), equalTo(FsRepository.TYPE));

        final Repository originalRepository = repositoriesService.repository(repositoryName);
        assertThat(originalRepository, instanceOf(FsRepository.class));

        final boolean updated = randomBoolean();
        final String updatedRepositoryType = updated ? "mock" : FsRepository.TYPE;

        assertAcked(client.admin().cluster().preparePutRepository(repositoryName)
                .setType(updatedRepositoryType)
                .setSettings(repoSettings)
                .get());

        final GetRepositoriesResponse updatedGetRepositoriesResponse =
            client.admin().cluster().prepareGetRepositories(repositoryName).get();

        assertThat(updatedGetRepositoriesResponse.repositories(), hasSize(1));
        final RepositoryMetadata updatedRepositoryMetadata = updatedGetRepositoriesResponse.repositories().get(0);

        assertThat(updatedRepositoryMetadata.type(), equalTo(updatedRepositoryType));

        final Repository updatedRepository = repositoriesService.repository(repositoryName);
        assertThat(updatedRepository, updated ? not(sameInstance(originalRepository)) : sameInstance(originalRepository));
    }
}
