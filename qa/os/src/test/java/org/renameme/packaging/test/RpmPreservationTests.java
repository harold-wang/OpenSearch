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

package org.renameme.packaging.test;

import org.renameme.packaging.util.Distribution;
import org.renameme.packaging.util.Shell;
import org.junit.BeforeClass;
import org.renameme.packaging.util.FileExistenceMatchers;
import org.renameme.packaging.util.FileUtils;
import org.renameme.packaging.util.Packages;
import org.renameme.packaging.util.Platforms;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeTrue;

public class RpmPreservationTests extends PackagingTestCase {

    @BeforeClass
    public static void filterDistros() {
        assumeTrue("only rpm", distribution.packaging == Distribution.Packaging.RPM);
        assumeTrue("only bundled jdk", distribution().hasJdk);
    }

    public void test10Install() throws Exception {
        Packages.assertRemoved(distribution());
        installation = Packages.installPackage(sh, distribution());
        Packages.assertInstalled(distribution());
        Packages.verifyPackageInstallation(installation, distribution(), sh);
    }

    public void test20Remove() throws Exception {
        Packages.remove(distribution());

        // config was removed
        assertThat(installation.config, FileExistenceMatchers.fileDoesNotExist());

        // sysvinit service file was removed
        assertThat(Packages.SYSVINIT_SCRIPT, FileExistenceMatchers.fileDoesNotExist());

        // defaults file was removed
        assertThat(installation.envFile, FileExistenceMatchers.fileDoesNotExist());
    }

    public void test30PreserveConfig() throws Exception {
        final Shell sh = new Shell();

        installation = Packages.installPackage(sh, distribution());
        Packages.assertInstalled(distribution());
        Packages.verifyPackageInstallation(installation, distribution(), sh);

        sh.run("echo foobar | " + installation.executables().keystoreTool + " add --stdin foo.bar");
        Stream.of("renameme.yml", "jvm.options", "log4j2.properties")
            .map(each -> installation.config(each))
            .forEach(path -> FileUtils.append(path, "# foo"));
        FileUtils.append(installation.config(Paths.get("jvm.options.d", "heap.options")), "# foo");
        if (distribution().isDefault()) {
            Stream.of("role_mapping.yml", "roles.yml", "users", "users_roles")
                .map(each -> installation.config(each))
                .forEach(path -> FileUtils.append(path, "# foo"));
        }

        Packages.remove(distribution());
        Packages.assertRemoved(distribution());

        if (Platforms.isSystemd()) {
            assertThat(sh.runIgnoreExitCode("systemctl is-enabled renameme.service").exitCode, is(1));
        }

        FileUtils.assertPathsDoNotExist(
            installation.bin,
            installation.lib,
            installation.modules,
            installation.plugins,
            installation.logs,
            installation.pidDir,
            installation.envFile,
            Packages.SYSVINIT_SCRIPT,
            Packages.SYSTEMD_SERVICE
        );

        assertThat(installation.config, FileExistenceMatchers.fileExists());
        assertThat(installation.config("renameme.keystore"), FileExistenceMatchers.fileExists());

        Stream.of("renameme.yml", "jvm.options", "log4j2.properties").forEach(this::assertConfFilePreserved);
        assertThat(installation.config(Paths.get("jvm.options.d", "heap.options")), FileExistenceMatchers.fileExists());

        if (distribution().isDefault()) {
            Stream.of("role_mapping.yml", "roles.yml", "users", "users_roles").forEach(this::assertConfFilePreserved);
        }
    }

    private void assertConfFilePreserved(String configFile) {
        final Path original = installation.config(configFile);
        final Path saved = installation.config(configFile + ".rpmsave");
        assertConfFilePreserved(original, saved);
    }

    private void assertConfFilePreserved(final Path original, final Path saved) {
        assertThat(original, FileExistenceMatchers.fileDoesNotExist());
        assertThat(saved, FileExistenceMatchers.fileExists());
    }

}
