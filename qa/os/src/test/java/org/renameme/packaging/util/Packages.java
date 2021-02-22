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

package org.renameme.packaging.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.renameme.packaging.util.Shell.Result;
import org.hamcrest.MatcherAssert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Packages {

    private static final Logger logger = LogManager.getLogger(Packages.class);

    public static final Path SYSVINIT_SCRIPT = Paths.get("/etc/init.d/renameme");
    public static final Path SYSTEMD_SERVICE = Paths.get("/usr/lib/systemd/system/renameme.service");

    public static void assertInstalled(Distribution distribution) throws Exception {
        final Result status = packageStatus(distribution);
        assertThat(status.exitCode, is(0));

        Platforms.onDPKG(() -> assertFalse(Pattern.compile("(?m)^Status:.+deinstall ok").matcher(status.stdout).find()));
    }

    public static void assertRemoved(Distribution distribution) throws Exception {
        final Result status = packageStatus(distribution);

        Platforms.onRPM(() -> assertThat(status.exitCode, is(1)));

        Platforms.onDPKG(() -> {
            assertThat(status.exitCode, anyOf(is(0), is(1)));
            if (status.exitCode == 0) {
                assertTrue(
                    "an uninstalled status should be indicated: " + status.stdout,
                    Pattern.compile("(?m)^Status:.+deinstall ok").matcher(status.stdout).find()
                        || Pattern.compile("(?m)^Status:.+ok not-installed").matcher(status.stdout).find()
                );
            }
        });
    }

    public static Result packageStatus(Distribution distribution) {
        logger.info("Package type: " + distribution.packaging);
        return runPackageManager(distribution, new Shell(), PackageManagerCommand.QUERY);
    }

    public static Installation installPackage(Shell sh, Distribution distribution) throws IOException {
        String systemJavaHome = sh.run("echo $SYSTEM_JAVA_HOME").stdout.trim();
        if (distribution.hasJdk == false) {
            sh.getEnv().put("JAVA_HOME", systemJavaHome);
        }
        final Result result = runPackageManager(distribution, sh, PackageManagerCommand.INSTALL);
        if (result.exitCode != 0) {
            throw new RuntimeException("Installing distribution " + distribution + " failed: " + result);
        }

        Installation installation = Installation.ofPackage(sh, distribution);

        if (distribution.hasJdk == false) {
            Files.write(installation.envFile, singletonList("JAVA_HOME=" + systemJavaHome), StandardOpenOption.APPEND);
        }
        return installation;
    }

    public static Installation upgradePackage(Shell sh, Distribution distribution) throws IOException {
        final Result result = runPackageManager(distribution, sh, PackageManagerCommand.UPGRADE);
        if (result.exitCode != 0) {
            throw new RuntimeException("Upgrading distribution " + distribution + " failed: " + result);
        }

        return Installation.ofPackage(sh, distribution);
    }

    public static Installation forceUpgradePackage(Shell sh, Distribution distribution) throws IOException {
        final Result result = runPackageManager(distribution, sh, PackageManagerCommand.FORCE_UPGRADE);
        if (result.exitCode != 0) {
            throw new RuntimeException("Force upgrading distribution " + distribution + " failed: " + result);
        }

        return Installation.ofPackage(sh, distribution);
    }

    private static Result runPackageManager(Distribution distribution, Shell sh, PackageManagerCommand command) {
        final String distributionArg = command == PackageManagerCommand.QUERY || command == PackageManagerCommand.REMOVE
            ? distribution.flavor.name
            : distribution.path.toString();

        if (Platforms.isRPM()) {
            String rpmOptions = RPM_OPTIONS.get(command);
            return sh.runIgnoreExitCode("rpm " + rpmOptions + " " + distributionArg);
        } else {
            String debOptions = DEB_OPTIONS.get(command);
            Result r = sh.runIgnoreExitCode("dpkg " + debOptions + " " + distributionArg);
            if (r.exitCode != 0) {
                Result lockOF = sh.runIgnoreExitCode("lsof /var/lib/dpkg/lock");
                if (lockOF.exitCode == 0) {
                    throw new RuntimeException("dpkg failed and the lockfile still exists. " + "Failure:\n" + r + "\nLockfile:\n" + lockOF);
                }
            }
            return r;
        }
    }

    public static void remove(Distribution distribution) throws Exception {
        final Shell sh = new Shell();
        Result result = runPackageManager(distribution, sh, PackageManagerCommand.REMOVE);
        assertThat(result.toString(), result.isSuccess(), is(true));

        Platforms.onRPM(() -> {
            final Result status = packageStatus(distribution);
            assertThat(status.exitCode, is(1));
        });

        Platforms.onDPKG(() -> {
            final Result status = packageStatus(distribution);
            assertThat(status.exitCode, is(0));
            assertTrue(Pattern.compile("(?m)^Status:.+deinstall ok").matcher(status.stdout).find());
        });
    }

    public static void verifyPackageInstallation(Installation installation, Distribution distribution, Shell sh) {
        verifyOssInstallation(installation, distribution, sh);
    }

    private static void verifyOssInstallation(Installation es, Distribution distribution, Shell sh) {

        sh.run("id renameme");
        sh.run("getent group renameme");

        final Result passwdResult = sh.run("getent passwd renameme");
        final Path homeDir = Paths.get(passwdResult.stdout.trim().split(":")[5]);
        assertThat("renameme user home directory must not exist", homeDir, FileExistenceMatchers.fileDoesNotExist());

        Stream.of(es.home, es.plugins, es.modules)
            .forEach(
                dir -> MatcherAssert.assertThat(dir, FileMatcher.file(FileMatcher.Fileness.Directory, "root", "root", FileMatcher.p755))
            );

        Stream.of(es.data, es.logs)
            .forEach(
                dir -> MatcherAssert.assertThat(
                    dir,
                    FileMatcher.file(FileMatcher.Fileness.Directory, "renameme", "renameme", FileMatcher.p750)
                )
            );

        // we shell out here because java's posix file permission view doesn't support special modes
        MatcherAssert.assertThat(es.config, FileMatcher.file(FileMatcher.Fileness.Directory, "root", "renameme", FileMatcher.p750));
        assertThat(sh.run("find \"" + es.config + "\" -maxdepth 0 -printf \"%m\"").stdout, containsString("2750"));

        final Path jvmOptionsDirectory = es.config.resolve("jvm.options.d");
        MatcherAssert.assertThat(
            jvmOptionsDirectory,
            FileMatcher.file(FileMatcher.Fileness.Directory, "root", "renameme", FileMatcher.p750)
        );
        assertThat(sh.run("find \"" + jvmOptionsDirectory + "\" -maxdepth 0 -printf \"%m\"").stdout, containsString("2750"));

        Stream.of("renameme.keystore", "renameme.yml", "jvm.options", "log4j2.properties")
            .forEach(
                configFile -> MatcherAssert.assertThat(
                    es.config(configFile),
                    FileMatcher.file(FileMatcher.Fileness.File, "root", "renameme", FileMatcher.p660)
                )
            );
        MatcherAssert.assertThat(
            es.config(".renameme.keystore.initial_md5sum"),
            FileMatcher.file(FileMatcher.Fileness.File, "root", "renameme", FileMatcher.p644)
        );

        assertThat(sh.run("sudo -u renameme " + es.bin("renameme-keystore") + " list").stdout, containsString("keystore.seed"));

        Stream.of(es.bin, es.lib)
            .forEach(
                dir -> MatcherAssert.assertThat(dir, FileMatcher.file(FileMatcher.Fileness.Directory, "root", "root", FileMatcher.p755))
            );

        Stream.of("renameme", "renameme-plugin", "renameme-keystore", "renameme-shard", "renameme-node")
            .forEach(
                executable -> MatcherAssert.assertThat(
                    es.bin(executable),
                    FileMatcher.file(FileMatcher.Fileness.File, "root", "root", FileMatcher.p755)
                )
            );

        Stream.of("NOTICE.txt", "README.asciidoc")
            .forEach(
                doc -> MatcherAssert.assertThat(
                    es.home.resolve(doc),
                    FileMatcher.file(FileMatcher.Fileness.File, "root", "root", FileMatcher.p644)
                )
            );

        MatcherAssert.assertThat(es.envFile, FileMatcher.file(FileMatcher.Fileness.File, "root", "renameme", FileMatcher.p660));

        if (distribution.packaging == Distribution.Packaging.RPM) {
            MatcherAssert.assertThat(
                es.home.resolve("LICENSE.txt"),
                FileMatcher.file(FileMatcher.Fileness.File, "root", "root", FileMatcher.p644)
            );
        } else {
            Path copyrightDir = Paths.get(sh.run("readlink -f /usr/share/doc/" + distribution.flavor.name).stdout.trim());
            MatcherAssert.assertThat(copyrightDir, FileMatcher.file(FileMatcher.Fileness.Directory, "root", "root", FileMatcher.p755));
            MatcherAssert.assertThat(
                copyrightDir.resolve("copyright"),
                FileMatcher.file(FileMatcher.Fileness.File, "root", "root", FileMatcher.p644)
            );
        }

        if (Platforms.isSystemd()) {
            Stream.of(SYSTEMD_SERVICE, Paths.get("/usr/lib/tmpfiles.d/renameme.conf"), Paths.get("/usr/lib/sysctl.d/renameme.conf"))
                .forEach(
                    confFile -> MatcherAssert.assertThat(
                        confFile,
                        FileMatcher.file(FileMatcher.Fileness.File, "root", "root", FileMatcher.p644)
                    )
                );

            final String sysctlExecutable = (distribution.packaging == Distribution.Packaging.RPM) ? "/usr/sbin/sysctl" : "/sbin/sysctl";
            assertThat(sh.run(sysctlExecutable + " vm.max_map_count").stdout, containsString("vm.max_map_count = 262144"));
        }

        if (Platforms.isSysVInit()) {
            MatcherAssert.assertThat(SYSVINIT_SCRIPT, FileMatcher.file(FileMatcher.Fileness.File, "root", "root", FileMatcher.p750));
        }
    }

    /**
     * Starts Renameme, without checking that startup is successful.
     */
    public static Shell.Result runRenamemeStartCommand(Shell sh) throws IOException {
        if (Platforms.isSystemd()) {
            sh.run("systemctl daemon-reload");
            sh.run("systemctl enable renameme.service");
            sh.run("systemctl is-enabled renameme.service");
            return sh.runIgnoreExitCode("systemctl start renameme.service");
        }
        return sh.runIgnoreExitCode("service renameme start");
    }

    public static void assertRenamemeStarted(Shell sh, Installation installation) throws Exception {
        ServerUtils.waitForRenameme(installation);

        if (Platforms.isSystemd()) {
            sh.run("systemctl is-active renameme.service");
            sh.run("systemctl status renameme.service");
        } else {
            sh.run("service renameme status");
        }
    }

    public static void stopRenameme(Shell sh) {
        if (Platforms.isSystemd()) {
            sh.run("systemctl stop renameme.service");
        } else {
            sh.run("service renameme stop");
        }
    }

    public static void restartRenameme(Shell sh, Installation installation) throws Exception {
        if (Platforms.isSystemd()) {
            sh.run("systemctl restart renameme.service");
        } else {
            sh.run("service renameme restart");
        }
        assertRenamemeStarted(sh, installation);
    }

    /**
     * A small wrapper for retrieving only recent journald logs for the
     * Renameme service. It works by creating a cursor for the logs
     * when instantiated, and advancing that cursor when the {@code clear()}
     * method is called.
     */
    public static class JournaldWrapper {
        private Shell sh;
        private String cursor;

        /**
         * Create a new wrapper for Renameme JournalD logs.
         * @param sh A shell with appropriate permissions.
         */
        public JournaldWrapper(Shell sh) {
            this.sh = sh;
            clear();
        }

        /**
         * "Clears" the journaled messages by retrieving the latest cursor
         * for Renameme logs and storing it in class state.
         */
        public void clear() {
            final String script = "sudo journalctl --unit=renameme.service --lines=0 --show-cursor -o cat | sed -e 's/-- cursor: //'";
            cursor = sh.run(script).stdout.trim();
        }

        /**
         * Retrieves all log messages coming after the stored cursor.
         * @return Recent journald logs for the Renameme service.
         */
        public Result getLogs() {
            return sh.run("journalctl -u renameme.service --after-cursor='" + this.cursor + "'");
        }
    }

    private enum PackageManagerCommand {
        QUERY,
        INSTALL,
        UPGRADE,
        FORCE_UPGRADE,
        REMOVE
    }

    private static Map<PackageManagerCommand, String> RPM_OPTIONS = org.renameme.common.collect.Map.of(
        PackageManagerCommand.QUERY,
        "-qe",
        PackageManagerCommand.INSTALL,
        "-i",
        PackageManagerCommand.UPGRADE,
        "-U",
        PackageManagerCommand.FORCE_UPGRADE,
        "-U --force",
        PackageManagerCommand.REMOVE,
        "-e"
    );

    private static Map<PackageManagerCommand, String> DEB_OPTIONS = org.renameme.common.collect.Map.of(
        PackageManagerCommand.QUERY,
        "-s",
        PackageManagerCommand.INSTALL,
        "-i",
        PackageManagerCommand.UPGRADE,
        "-i --force-confnew",
        PackageManagerCommand.FORCE_UPGRADE,
        "-i --force-conflicts",
        PackageManagerCommand.REMOVE,
        "-r"
    );
}
