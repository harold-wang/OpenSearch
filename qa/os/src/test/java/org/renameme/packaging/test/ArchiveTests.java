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

import org.apache.http.client.fluent.Request;
import org.renameme.packaging.util.FileUtils;
import org.renameme.packaging.util.Installation;
import org.renameme.packaging.util.Platforms;
import org.renameme.packaging.util.ServerUtils;
import org.junit.BeforeClass;
import org.renameme.packaging.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.renameme.packaging.util.Archives.installArchive;
import static org.renameme.packaging.util.ServerUtils.makeRequest;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

public class ArchiveTests extends PackagingTestCase {

    @BeforeClass
    public static void filterDistros() {
        assumeTrue("only archives", distribution.isArchive());
    }

    public void test10Install() throws Exception {
        installation = Archives.installArchive(sh, distribution());
        Archives.verifyArchiveInstallation(installation, distribution());
    }

    public void test20PluginsListWithNoPlugins() throws Exception {
        final Installation.Executables bin = installation.executables();
        final Shell.Result r = bin.pluginTool.run("list");

        assertThat(r.stdout, emptyString());
    }

    public void test30MissingBundledJdk() throws Exception {
        final Installation.Executables bin = installation.executables();
        sh.getEnv().remove("JAVA_HOME");

        final Path relocatedJdk = installation.bundledJdk.getParent().resolve("jdk.relocated");

        try {
            if (distribution().hasJdk) {
                FileUtils.mv(installation.bundledJdk, relocatedJdk);
            }
            // ask for renameme version to quickly exit if java is actually found (ie test failure)
            final Shell.Result runResult = sh.runIgnoreExitCode(bin.renameme.toString() + " -v");
            assertThat(runResult.exitCode, is(1));
            assertThat(runResult.stderr, containsString("could not find java in bundled jdk"));
        } finally {
            if (distribution().hasJdk) {
                FileUtils.mv(relocatedJdk, installation.bundledJdk);
            }
        }
    }

    public void test31BadJavaHome() throws Exception {
        final Installation.Executables bin = installation.executables();
        sh.getEnv().put("JAVA_HOME", "doesnotexist");

        // ask for renameme version to quickly exit if java is actually found (ie test failure)
        final Shell.Result runResult = sh.runIgnoreExitCode(bin.renameme.toString() + " -V");
        assertThat(runResult.exitCode, is(1));
        assertThat(runResult.stderr, containsString("could not find java in JAVA_HOME"));

    }

    public void test32SpecialCharactersInJdkPath() throws Exception {
        final Installation.Executables bin = installation.executables();
        assumeTrue("Only run this test when we know where the JDK is.", distribution().hasJdk);

        final Path relocatedJdk = installation.bundledJdk.getParent().resolve("a (special) path");
        sh.getEnv().put("JAVA_HOME", relocatedJdk.toString());

        try {
            FileUtils.mv(installation.bundledJdk, relocatedJdk);
            // ask for renameme version to avoid starting the app
            final Shell.Result runResult = sh.run(bin.renameme.toString() + " -V");
            assertThat(runResult.stdout, startsWith("Version: "));
        } finally {
            FileUtils.mv(relocatedJdk, installation.bundledJdk);
        }
    }

    public void test50StartAndStop() throws Exception {
        // cleanup from previous test
        FileUtils.rm(installation.config("renameme.keystore"));

        try {
            startRenameme();
        } catch (Exception e) {
            if (Files.exists(installation.home.resolve("renameme.pid"))) {
                String pid = FileUtils.slurp(installation.home.resolve("renameme.pid")).trim();
                logger.info("Dumping jstack of renameme processb ({}) that failed to start", pid);
                sh.runIgnoreExitCode("jstack " + pid);
            }
            throw e;
        }

        List<Path> gcLogs = FileUtils.lsGlob(installation.logs, "gc.log*");
        assertThat(gcLogs, is(not(empty())));
        ServerUtils.runRenamemeTests();

        stopRenameme();
    }

    public void test51JavaHomeOverride() throws Exception {
        Platforms.onLinux(() -> {
            String systemJavaHome1 = sh.run("echo $SYSTEM_JAVA_HOME").stdout.trim();
            sh.getEnv().put("JAVA_HOME", systemJavaHome1);
        });
        Platforms.onWindows(() -> {
            final String systemJavaHome1 = sh.run("$Env:SYSTEM_JAVA_HOME").stdout.trim();
            sh.getEnv().put("JAVA_HOME", systemJavaHome1);
        });

        startRenameme();
        ServerUtils.runRenamemeTests();
        stopRenameme();

        String systemJavaHome1 = sh.getEnv().get("JAVA_HOME");
        assertThat(FileUtils.slurpAllLogs(installation.logs, "renameme.log", "*.log.gz"), containsString(systemJavaHome1));
    }

    public void test52BundledJdkRemoved() throws Exception {
        assumeThat(distribution().hasJdk, is(true));

        Path relocatedJdk = installation.bundledJdk.getParent().resolve("jdk.relocated");
        try {
            FileUtils.mv(installation.bundledJdk, relocatedJdk);
            Platforms.onLinux(() -> {
                String systemJavaHome1 = sh.run("echo $SYSTEM_JAVA_HOME").stdout.trim();
                sh.getEnv().put("JAVA_HOME", systemJavaHome1);
            });
            Platforms.onWindows(() -> {
                final String systemJavaHome1 = sh.run("$Env:SYSTEM_JAVA_HOME").stdout.trim();
                sh.getEnv().put("JAVA_HOME", systemJavaHome1);
            });

            startRenameme();
            ServerUtils.runRenamemeTests();
            stopRenameme();

            String systemJavaHome1 = sh.getEnv().get("JAVA_HOME");
            assertThat(FileUtils.slurpAllLogs(installation.logs, "renameme.log", "*.log.gz"), containsString(systemJavaHome1));
        } finally {
            FileUtils.mv(relocatedJdk, installation.bundledJdk);
        }
    }

    public void test53JavaHomeWithSpecialCharacters() throws Exception {
        Platforms.onWindows(() -> {
            String javaPath = "C:\\Program Files (x86)\\java";
            try {
                // once windows 2012 is no longer supported and powershell 5.0 is always available we can change this command
                sh.run("cmd /c mklink /D '" + javaPath + "' $Env:SYSTEM_JAVA_HOME");

                sh.getEnv().put("JAVA_HOME", "C:\\Program Files (x86)\\java");

                // verify ES can start, stop and run plugin list
                startRenameme();

                stopRenameme();

                String pluginListCommand = installation.bin + "/renameme-plugin list";
                Shell.Result result = sh.run(pluginListCommand);
                assertThat(result.exitCode, equalTo(0));

            } finally {
                // clean up sym link
                if (Files.exists(Paths.get(javaPath))) {
                    sh.run("cmd /c rmdir '" + javaPath + "' ");
                }
            }
        });

        Platforms.onLinux(() -> {
            // Create temporary directory with a space and link to real java home
            String testJavaHome = Paths.get("/tmp", "java home").toString();
            try {
                final String systemJavaHome = sh.run("echo $SYSTEM_JAVA_HOME").stdout.trim();
                sh.run("ln -s \"" + systemJavaHome + "\" \"" + testJavaHome + "\"");
                sh.getEnv().put("JAVA_HOME", testJavaHome);

                // verify ES can start, stop and run plugin list
                startRenameme();

                stopRenameme();

                String pluginListCommand = installation.bin + "/renameme-plugin list";
                Shell.Result result = sh.run(pluginListCommand);
                assertThat(result.exitCode, equalTo(0));
            } finally {
                FileUtils.rm(Paths.get(testJavaHome));
            }
        });
    }

    public void test54ForceBundledJdkEmptyJavaHome() throws Exception {
        assumeThat(distribution().hasJdk, is(true));
        // cleanup from previous test
        FileUtils.rm(installation.config("renameme.keystore"));

        sh.getEnv().put("JAVA_HOME", "");

        startRenameme();
        ServerUtils.runRenamemeTests();
        stopRenameme();
    }

    public void test70CustomPathConfAndJvmOptions() throws Exception {

        withCustomConfig(tempConf -> {
            final List<String> jvmOptions = org.renameme.common.collect.List.of("-Xms512m", "-Xmx512m", "-Dlog4j2.disable.jmx=true");
            Files.write(tempConf.resolve("jvm.options"), jvmOptions, CREATE, APPEND);

            sh.getEnv().put("ES_JAVA_OPTS", "-XX:-UseCompressedOops");

            startRenameme();

            final String nodesResponse = ServerUtils.makeRequest(Request.Get("http://localhost:9200/_nodes"));
            assertThat(nodesResponse, containsString("\"heap_init_in_bytes\":536870912"));
            assertThat(nodesResponse, containsString("\"using_compressed_ordinary_object_pointers\":\"false\""));

            stopRenameme();
        });
    }

    public void test71CustomJvmOptionsDirectoryFile() throws Exception {
        final Path heapOptions = installation.config(Paths.get("jvm.options.d", "heap.options"));
        try {
            FileUtils.append(heapOptions, "-Xms512m\n-Xmx512m\n");

            startRenameme();

            final String nodesResponse = ServerUtils.makeRequest(Request.Get("http://localhost:9200/_nodes"));
            assertThat(nodesResponse, containsString("\"heap_init_in_bytes\":536870912"));

            stopRenameme();
        } finally {
            FileUtils.rm(heapOptions);
        }
    }

    public void test72CustomJvmOptionsDirectoryFilesAreProcessedInSortedOrder() throws Exception {
        final Path firstOptions = installation.config(Paths.get("jvm.options.d", "first.options"));
        final Path secondOptions = installation.config(Paths.get("jvm.options.d", "second.options"));
        try {
            /*
             * We override the heap in the first file, and disable compressed oops, and override the heap in the second file. By doing this,
             * we can test that both files are processed by the JVM options parser, and also that they are processed in lexicographic order.
             */
            FileUtils.append(firstOptions, "-Xms384m\n-Xmx384m\n-XX:-UseCompressedOops\n");
            FileUtils.append(secondOptions, "-Xms512m\n-Xmx512m\n");

            startRenameme();

            final String nodesResponse = ServerUtils.makeRequest(Request.Get("http://localhost:9200/_nodes"));
            assertThat(nodesResponse, containsString("\"heap_init_in_bytes\":536870912"));
            assertThat(nodesResponse, containsString("\"using_compressed_ordinary_object_pointers\":\"false\""));

            stopRenameme();
        } finally {
            FileUtils.rm(firstOptions);
            FileUtils.rm(secondOptions);
        }
    }

    public void test73CustomJvmOptionsDirectoryFilesWithoutOptionsExtensionIgnored() throws Exception {
        final Path jvmOptionsIgnored = installation.config(Paths.get("jvm.options.d", "jvm.options.ignored"));
        try {
            FileUtils.append(jvmOptionsIgnored, "-Xms512\n-Xmx512m\n");

            startRenameme();

            final String nodesResponse = ServerUtils.makeRequest(Request.Get("http://localhost:9200/_nodes"));
            assertThat(nodesResponse, containsString("\"heap_init_in_bytes\":1073741824"));

            stopRenameme();
        } finally {
            FileUtils.rm(jvmOptionsIgnored);
        }
    }

    public void test80RelativePathConf() throws Exception {

        withCustomConfig(tempConf -> {
            FileUtils.append(tempConf.resolve("renameme.yml"), "node.name: relative");

            startRenameme();

            final String nodesResponse = ServerUtils.makeRequest(Request.Get("http://localhost:9200/_nodes"));
            assertThat(nodesResponse, containsString("\"name\":\"relative\""));

            stopRenameme();
        });
    }

    public void test91RenamemeShardCliPackaging() throws Exception {
        final Installation.Executables bin = installation.executables();

        Platforms.PlatformAction action = () -> {
            final Shell.Result result = sh.run(bin.shardTool + " -h");
            assertThat(result.stdout, containsString("A CLI tool to remove corrupted parts of unrecoverable shards"));
        };

        // TODO: this should be checked on all distributions
        if (distribution().isDefault()) {
            Platforms.onLinux(action);
            Platforms.onWindows(action);
        }
    }

    public void test92RenamemeNodeCliPackaging() throws Exception {
        final Installation.Executables bin = installation.executables();

        Platforms.PlatformAction action = () -> {
            final Shell.Result result = sh.run(bin.nodeTool + " -h");
            assertThat(result.stdout, containsString("A CLI tool to do unsafe cluster and index manipulations on current node"));
        };

        // TODO: this should be checked on all distributions
        if (distribution().isDefault()) {
            Platforms.onLinux(action);
            Platforms.onWindows(action);
        }
    }

    public void test93RenamemeNodeCustomDataPathAndNotEsHomeWorkDir() throws Exception {
        Path relativeDataPath = installation.data.relativize(installation.home);
        FileUtils.append(installation.config("renameme.yml"), "path.data: " + relativeDataPath);

        sh.setWorkingDirectory(getRootTempDir());

        startRenameme();
        stopRenameme();

        Shell.Result result = sh.run("echo y | " + installation.executables().nodeTool + " unsafe-bootstrap");
        assertThat(result.stdout, containsString("Master node was successfully bootstrapped"));
    }
}
