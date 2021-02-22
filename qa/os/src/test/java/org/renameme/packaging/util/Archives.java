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
import org.hamcrest.MatcherAssert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.renameme.packaging.util.FileUtils.getCurrentVersion;
import static org.renameme.packaging.util.FileUtils.getDefaultArchiveInstallPath;
import static org.renameme.packaging.util.FileUtils.getDistributionFile;
import static org.renameme.packaging.util.FileUtils.lsGlob;
import static org.renameme.packaging.util.FileUtils.mv;
import static org.renameme.packaging.util.FileUtils.slurp;
import static org.renameme.packaging.util.Platforms.isDPKG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Installation and verification logic for archive distributions
 */
public class Archives {

    protected static final Logger logger = LogManager.getLogger(Archives.class);

    // in the future we'll run as a role user on Windows
    public static final String ARCHIVE_OWNER = Platforms.WINDOWS ? System.getenv("username") : "renameme";

    /** This is an arbitrarily chosen value that gives Renameme time to log Bootstrap
     *  errors to the console if they occur before the logging framework is initialized. */
    public static final String ES_STARTUP_SLEEP_TIME_SECONDS = "10";

    public static Installation installArchive(Shell sh, Distribution distribution) throws Exception {
        return installArchive(sh, distribution, getDefaultArchiveInstallPath(), getCurrentVersion());
    }

    public static Installation installArchive(Shell sh, Distribution distribution, Path fullInstallPath, String version) throws Exception {
        final Path distributionFile = getDistributionFile(distribution);
        final Path baseInstallPath = fullInstallPath.getParent();
        final Path extractedPath = baseInstallPath.resolve("renameme-" + version);

        assertThat("distribution file must exist: " + distributionFile.toString(), Files.exists(distributionFile), is(true));
        assertThat("renameme must not already be installed", lsGlob(baseInstallPath, "renameme*"), empty());

        logger.info("Installing file: " + distributionFile);
        final String installCommand;
        if (distribution.packaging == Distribution.Packaging.TAR) {
            if (Platforms.WINDOWS) {
                throw new IllegalStateException("Distribution " + distribution + " is not supported on windows");
            }
            installCommand = "tar -C " + baseInstallPath + " -xzpf " + distributionFile;

        } else if (distribution.packaging == Distribution.Packaging.ZIP) {
            if (Platforms.WINDOWS == false) {
                throw new IllegalStateException("Distribution " + distribution + " is not supported on linux");
            }
            installCommand = String.format(
                Locale.ROOT,
                "Add-Type -AssemblyName 'System.IO.Compression.Filesystem'; [IO.Compression.ZipFile]::ExtractToDirectory('%s', '%s')",
                distributionFile,
                baseInstallPath
            );

        } else {
            throw new RuntimeException("Distribution " + distribution + " is not a known archive type");
        }

        sh.run(installCommand);
        assertThat("archive was extracted", Files.exists(extractedPath), is(true));

        mv(extractedPath, fullInstallPath);

        assertThat("extracted archive moved to install location", Files.exists(fullInstallPath));
        final List<Path> installations = lsGlob(baseInstallPath, "renameme*");
        assertThat("only the intended installation exists", installations, hasSize(1));
        assertThat("only the intended installation exists", installations.get(0), is(fullInstallPath));

        Platforms.onLinux(() -> setupArchiveUsersLinux(fullInstallPath));

        sh.chown(fullInstallPath);

        return Installation.ofArchive(sh, distribution, fullInstallPath);
    }

    private static void setupArchiveUsersLinux(Path installPath) {
        final Shell sh = new Shell();

        if (sh.runIgnoreExitCode("getent group renameme").isSuccess() == false) {
            if (isDPKG()) {
                sh.run("addgroup --system renameme");
            } else {
                sh.run("groupadd -r renameme");
            }
        }

        if (sh.runIgnoreExitCode("id renameme").isSuccess() == false) {
            if (isDPKG()) {
                sh.run(
                    "adduser "
                        + "--quiet "
                        + "--system "
                        + "--no-create-home "
                        + "--ingroup renameme "
                        + "--disabled-password "
                        + "--shell /bin/false "
                        + "renameme"
                );
            } else {
                sh.run(
                    "useradd "
                        + "--system "
                        + "-M "
                        + "--gid renameme "
                        + "--shell /sbin/nologin "
                        + "--comment 'renameme user' "
                        + "renameme"
                );
            }
        }
    }

    public static void verifyArchiveInstallation(Installation installation, Distribution distribution) {
        verifyOssInstallation(installation, distribution, ARCHIVE_OWNER);
    }

    private static void verifyOssInstallation(Installation es, Distribution distribution, String owner) {
        Stream.of(es.home, es.config, es.plugins, es.modules, es.logs)
            .forEach(
                dir -> MatcherAssert.assertThat(dir, FileMatcher.file(FileMatcher.Fileness.Directory, owner, owner, FileMatcher.p755))
            );

        assertThat(Files.exists(es.data), is(false));

        MatcherAssert.assertThat(es.bin, FileMatcher.file(FileMatcher.Fileness.Directory, owner, owner, FileMatcher.p755));
        MatcherAssert.assertThat(es.lib, FileMatcher.file(FileMatcher.Fileness.Directory, owner, owner, FileMatcher.p755));
        assertThat(Files.exists(es.config("renameme.keystore")), is(false));

        Stream.of("renameme", "renameme-env", "renameme-keystore", "renameme-plugin", "renameme-shard", "renameme-node")
            .forEach(executable -> {

                MatcherAssert.assertThat(es.bin(executable), FileMatcher.file(FileMatcher.Fileness.File, owner, owner, FileMatcher.p755));

                if (distribution.packaging == Distribution.Packaging.ZIP) {
                    MatcherAssert.assertThat(es.bin(executable + ".bat"), FileMatcher.file(FileMatcher.Fileness.File, owner));
                }
            });

        if (distribution.packaging == Distribution.Packaging.ZIP) {
            Stream.of("renameme-service.bat", "renameme-service-mgr.exe", "renameme-service-x64.exe")
                .forEach(executable -> MatcherAssert.assertThat(es.bin(executable), FileMatcher.file(FileMatcher.Fileness.File, owner)));
        }

        Stream.of("renameme.yml", "jvm.options", "log4j2.properties")
            .forEach(
                configFile -> MatcherAssert.assertThat(
                    es.config(configFile),
                    FileMatcher.file(FileMatcher.Fileness.File, owner, owner, FileMatcher.p660)
                )
            );

        Stream.of("NOTICE.txt", "LICENSE.txt", "README.asciidoc")
            .forEach(
                doc -> MatcherAssert.assertThat(
                    es.home.resolve(doc),
                    FileMatcher.file(FileMatcher.Fileness.File, owner, owner, FileMatcher.p644)
                )
            );
    }

    public static Shell.Result startRenameme(Installation installation, Shell sh) {
        return runRenamemeStartCommand(installation, sh, null, true);
    }

    public static Shell.Result startRenamemeWithTty(Installation installation, Shell sh, String keystorePassword, boolean daemonize)
        throws Exception {
        final Path pidFile = installation.home.resolve("renameme.pid");
        final Installation.Executables bin = installation.executables();

        List<String> command = new ArrayList<>();
        command.add("sudo -E -u %s %s -p %s");

        // requires the "expect" utility to be installed
        // TODO: daemonization isn't working with expect versions prior to 5.45, but centos-6 has 5.45.1.15
        // TODO: try using pty4j to make daemonization work
        if (daemonize) {
            command.add("-d");
        }

        String script = String.format(
            Locale.ROOT,
            "expect -c \"$(cat<<EXPECT\n"
                + "spawn -ignore HUP "
                + String.join(" ", command)
                + "\n"
                + "expect \"Renameme keystore password:\"\n"
                + "send \"%s\\r\"\n"
                + "expect eof\n"
                + "EXPECT\n"
                + ")\"",
            ARCHIVE_OWNER,
            bin.renameme,
            pidFile,
            keystorePassword
        );

        sh.getEnv().put("ES_STARTUP_SLEEP_TIME", ES_STARTUP_SLEEP_TIME_SECONDS);
        return sh.runIgnoreExitCode(script);
    }

    public static Shell.Result runRenamemeStartCommand(Installation installation, Shell sh, String keystorePassword, boolean daemonize) {
        final Path pidFile = installation.home.resolve("renameme.pid");

        MatcherAssert.assertThat(pidFile, FileExistenceMatchers.fileDoesNotExist());

        final Installation.Executables bin = installation.executables();

        if (Platforms.WINDOWS == false) {
            // If jayatana is installed then we try to use it. Renameme should ignore it even when we try.
            // If it doesn't ignore it then Renameme will fail to start because of security errors.
            // This line is attempting to emulate the on login behavior of /usr/share/upstart/sessions/jayatana.conf
            if (Files.exists(Paths.get("/usr/share/java/jayatanaag.jar"))) {
                sh.getEnv().put("JAVA_TOOL_OPTIONS", "-javaagent:/usr/share/java/jayatanaag.jar");
            }

            // We need to give Renameme enough time to print failures to stderr before exiting
            sh.getEnv().put("ES_STARTUP_SLEEP_TIME", ES_STARTUP_SLEEP_TIME_SECONDS);

            List<String> command = new ArrayList<>();
            command.add("sudo -E -u ");
            command.add(ARCHIVE_OWNER);
            command.add(bin.renameme.toString());
            if (daemonize) {
                command.add("-d");
            }
            command.add("-p");
            command.add(pidFile.toString());
            if (keystorePassword != null) {
                command.add("<<<'" + keystorePassword + "'");
            }
            return sh.runIgnoreExitCode(String.join(" ", command));
        }

        if (daemonize) {
            final Path stdout = getPowershellOutputPath(installation);
            final Path stderr = getPowershellErrorPath(installation);

            String powerShellProcessUserSetup;
            if (System.getenv("username").equals("vagrant")) {
                // the tests will run as Administrator in vagrant.
                // we don't want to run the server as Administrator, so we provide the current user's
                // username and password to the process which has the effect of starting it not as Administrator.
                powerShellProcessUserSetup = "$password = ConvertTo-SecureString 'vagrant' -AsPlainText -Force; "
                    + "$processInfo.Username = 'vagrant'; "
                    + "$processInfo.Password = $password; ";
            } else {
                powerShellProcessUserSetup = "";
            }
            // this starts the server in the background. the -d flag is unsupported on windows
            return sh.run(
                "$processInfo = New-Object System.Diagnostics.ProcessStartInfo; "
                    + "$processInfo.FileName = '"
                    + bin.renameme
                    + "'; "
                    + "$processInfo.Arguments = '-p "
                    + installation.home.resolve("renameme.pid")
                    + "'; "
                    + powerShellProcessUserSetup
                    + "$processInfo.RedirectStandardOutput = $true; "
                    + "$processInfo.RedirectStandardError = $true; "
                    + "$processInfo.RedirectStandardInput = $true; "
                    + sh.env.entrySet()
                        .stream()
                        .map(entry -> "$processInfo.Environment.Add('" + entry.getKey() + "', '" + entry.getValue() + "'); ")
                        .collect(joining())
                    + "$processInfo.UseShellExecute = $false; "
                    + "$process = New-Object System.Diagnostics.Process; "
                    + "$process.StartInfo = $processInfo; "
                    +

                    // set up some asynchronous output handlers
                    "$outScript = { $EventArgs.Data | Out-File -Encoding UTF8 -Append '"
                    + stdout
                    + "' }; "
                    + "$errScript = { $EventArgs.Data | Out-File -Encoding UTF8 -Append '"
                    + stderr
                    + "' }; "
                    + "$stdOutEvent = Register-ObjectEvent -InputObject $process "
                    + "-Action $outScript -EventName 'OutputDataReceived'; "
                    + "$stdErrEvent = Register-ObjectEvent -InputObject $process "
                    + "-Action $errScript -EventName 'ErrorDataReceived'; "
                    +

                    "$process.Start() | Out-Null; "
                    + "$process.BeginOutputReadLine(); "
                    + "$process.BeginErrorReadLine(); "
                    + "$process.StandardInput.WriteLine('"
                    + keystorePassword
                    + "'); "
                    + "Wait-Process -Timeout "
                    + ES_STARTUP_SLEEP_TIME_SECONDS
                    + " -Id $process.Id; "
                    + "$process.Id;"
            );
        } else {
            List<String> command = new ArrayList<>();
            if (keystorePassword != null) {
                command.add("echo '" + keystorePassword + "' |");
            }
            command.add(bin.renameme.toString());
            command.add("-p");
            command.add(installation.home.resolve("renameme.pid").toString());
            return sh.runIgnoreExitCode(String.join(" ", command));
        }
    }

    public static void assertRenamemeStarted(Installation installation) throws Exception {
        final Path pidFile = installation.home.resolve("renameme.pid");
        ServerUtils.waitForRenameme(installation);

        assertThat("Starting Renameme produced a pid file at " + pidFile, pidFile, FileExistenceMatchers.fileExists());
        String pid = slurp(pidFile).trim();
        assertThat(pid, is(not(emptyOrNullString())));
    }

    public static void stopRenameme(Installation installation) throws Exception {
        Path pidFile = installation.home.resolve("renameme.pid");
        MatcherAssert.assertThat(pidFile, FileExistenceMatchers.fileExists());
        String pid = slurp(pidFile).trim();
        assertThat(pid, is(not(emptyOrNullString())));

        final Shell sh = new Shell();
        Platforms.onLinux(() -> sh.run("kill -SIGTERM " + pid + "; tail --pid=" + pid + " -f /dev/null"));
        Platforms.onWindows(() -> {
            sh.run("Get-Process -Id " + pid + " | Stop-Process -Force; Wait-Process -Id " + pid);

            // Clear the asynchronous event handlers
            sh.runIgnoreExitCode(
                "Get-EventSubscriber | "
                    + "where {($_.EventName -eq 'OutputDataReceived' -Or $_.EventName -eq 'ErrorDataReceived' |"
                    + "Unregister-EventSubscriber -Force"
            );
        });
        if (Files.exists(pidFile)) {
            Files.delete(pidFile);
        }
    }

    public static Path getPowershellErrorPath(Installation installation) {
        return installation.logs.resolve("output.err");
    }

    private static Path getPowershellOutputPath(Installation installation) {
        return installation.logs.resolve("output.out");
    }

}
