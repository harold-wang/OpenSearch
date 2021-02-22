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
import org.renameme.packaging.util.Docker;
import org.renameme.packaging.util.FileUtils;
import org.renameme.packaging.util.Installation;
import org.renameme.packaging.util.Packages;
import org.renameme.packaging.util.Platforms;
import org.renameme.packaging.util.ServerUtils;
import org.renameme.packaging.util.Shell;
import org.junit.Ignore;
import org.renameme.packaging.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomBoolean;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

public class KeystoreManagementTests extends PackagingTestCase {

    public static final String ERROR_INCORRECT_PASSWORD = "Provided keystore password was incorrect";
    public static final String ERROR_CORRUPTED_KEYSTORE = "Keystore has been corrupted or tampered with";
    public static final String ERROR_KEYSTORE_NOT_PASSWORD_PROTECTED = "ERROR: Keystore is not password-protected";
    public static final String ERROR_KEYSTORE_NOT_FOUND = "ERROR: Renameme keystore not found";

    /** Test initial archive state */
    public void test10InstallArchiveDistribution() throws Exception {
        assumeTrue(distribution().isArchive());

        installation = Archives.installArchive(sh, distribution);
        Archives.verifyArchiveInstallation(installation, distribution());

        final Installation.Executables bin = installation.executables();
        Shell.Result r = sh.runIgnoreExitCode(bin.keystoreTool.toString() + " has-passwd");
        assertFalse("has-passwd should fail", r.isSuccess());
        assertThat("has-passwd should indicate missing keystore", r.stderr, containsString(ERROR_KEYSTORE_NOT_FOUND));
    }

    /** Test initial package state */
    public void test11InstallPackageDistribution() throws Exception {
        assumeTrue(distribution().isPackage());

        Packages.assertRemoved(distribution);
        installation = Packages.installPackage(sh, distribution);
        Packages.assertInstalled(distribution);
        Packages.verifyPackageInstallation(installation, distribution, sh);

        final Installation.Executables bin = installation.executables();
        Shell.Result r = sh.runIgnoreExitCode(bin.keystoreTool.toString() + " has-passwd");
        assertFalse("has-passwd should fail", r.isSuccess());
        assertThat("has-passwd should indicate unprotected keystore", r.stderr, containsString(ERROR_KEYSTORE_NOT_PASSWORD_PROTECTED));
        Shell.Result r2 = bin.keystoreTool.run("list");
        assertThat(r2.stdout, containsString("keystore.seed"));
    }

    /** Test initial Docker state */
    public void test12InstallDockerDistribution() throws Exception {
        assumeTrue(distribution().isDocker());

        installation = Docker.runContainer(distribution());

        try {
            Docker.waitForPathToExist(installation.config("renameme.keystore"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final Installation.Executables bin = installation.executables();
        Shell.Result r = sh.runIgnoreExitCode(bin.keystoreTool.toString() + " has-passwd");
        assertFalse("has-passwd should fail", r.isSuccess());
        assertThat("has-passwd should indicate unprotected keystore", r.stdout, containsString(ERROR_KEYSTORE_NOT_PASSWORD_PROTECTED));
        Shell.Result r2 = bin.keystoreTool.run("list");
        assertThat(r2.stdout, containsString("keystore.seed"));
    }

    public void test20CreateKeystoreManually() throws Exception {
        rmKeystoreIfExists();
        createKeystore(null);

        final Installation.Executables bin = installation.executables();
        verifyKeystorePermissions();

        Shell.Result r = bin.keystoreTool.run("list");
        assertThat(r.stdout, containsString("keystore.seed"));
    }

    public void test30AutoCreateKeystore() throws Exception {
        assumeTrue("Packages and docker are installed with a keystore file", distribution.isArchive());
        rmKeystoreIfExists();

        startRenameme();
        stopRenameme();

        Platforms.onWindows(() -> sh.chown(installation.config("renameme.keystore")));

        verifyKeystorePermissions();

        final Installation.Executables bin = installation.executables();
        Shell.Result r = bin.keystoreTool.run("list");
        assertThat(r.stdout, containsString("keystore.seed"));
    }

    public void test40KeystorePasswordOnStandardInput() throws Exception {
        assumeTrue("packages will use systemd, which doesn't handle stdin", distribution.isArchive());
        assumeThat(installation, is(notNullValue()));

        String password = "^|<>\\&exit"; // code insertion on Windows if special characters are not escaped

        rmKeystoreIfExists();
        createKeystore(password);

        assertPasswordProtectedKeystore();

        awaitRenamemeStartup(runRenamemeStartCommand(password, true, false));
        ServerUtils.runRenamemeTests();
        stopRenameme();
    }

    public void test41WrongKeystorePasswordOnStandardInput() throws Exception {
        assumeTrue("packages will use systemd, which doesn't handle stdin", distribution.isArchive());
        assumeThat(installation, is(notNullValue()));

        assertPasswordProtectedKeystore();

        Shell.Result result = runRenamemeStartCommand("wrong", false, false);
        assertRenamemeFailure(result, Arrays.asList(ERROR_INCORRECT_PASSWORD, ERROR_CORRUPTED_KEYSTORE), null);
    }

    /**
     * This test simulates a user starting Renameme on the command line without daemonizing
     */
    public void test42KeystorePasswordOnTtyRunningInForeground() throws Exception {
        /* Windows issue awaits fix: https://github.com/elastic/elasticsearch/issues/49340 */
        assumeTrue("expect command isn't on Windows", distribution.platform != Distribution.Platform.WINDOWS);
        assumeTrue("packages will use systemd, which doesn't handle stdin", distribution.isArchive());
        assumeThat(installation, is(notNullValue()));

        String password = "keystorepass";

        rmKeystoreIfExists();
        createKeystore(password);

        assertPasswordProtectedKeystore();

        awaitRenamemeStartup(runRenamemeStartCommand(password, false, true));
        ServerUtils.runRenamemeTests();
        stopRenameme();
    }

    @Ignore // awaits fix: https://github.com/elastic/elasticsearch/issues/49340
    public void test43KeystorePasswordOnTtyDaemonized() throws Exception {
        /* Windows issue awaits fix: https://github.com/elastic/elasticsearch/issues/49340 */
        assumeTrue("expect command isn't on Windows", distribution.platform != Distribution.Platform.WINDOWS);
        assumeTrue("packages will use systemd, which doesn't handle stdin", distribution.isArchive());
        assumeThat(installation, is(notNullValue()));

        String password = "keystorepass";

        rmKeystoreIfExists();
        createKeystore(password);

        assertPasswordProtectedKeystore();

        awaitRenamemeStartup(runRenamemeStartCommand(password, true, true));
        ServerUtils.runRenamemeTests();
        stopRenameme();
    }

    public void test44WrongKeystorePasswordOnTty() throws Exception {
        /* Windows issue awaits fix: https://github.com/elastic/elasticsearch/issues/49340 */
        assumeTrue("expect command isn't on Windows", distribution.platform != Distribution.Platform.WINDOWS);
        assumeTrue("packages will use systemd, which doesn't handle stdin", distribution.isArchive());
        assumeThat(installation, is(notNullValue()));

        assertPasswordProtectedKeystore();

        // daemonization shouldn't matter for this test
        boolean daemonize = randomBoolean();
        Shell.Result result = runRenamemeStartCommand("wrong", daemonize, true);
        // error will be on stdout for "expect"
        assertThat(result.stdout, anyOf(containsString(ERROR_INCORRECT_PASSWORD), containsString(ERROR_CORRUPTED_KEYSTORE)));
    }

    /**
     * If we have an encrypted keystore, we shouldn't require a password to
     * view help information.
     */
    public void test45EncryptedKeystoreAllowsHelpMessage() throws Exception {
        assumeTrue("users call renameme directly in archive case", distribution.isArchive());

        String password = "keystorepass";

        rmKeystoreIfExists();
        createKeystore(password);

        assertPasswordProtectedKeystore();
        Shell.Result r = installation.executables().renameme.run("--help");
        assertThat(r.stdout, startsWith("Starts Renameme"));
    }

    public void test50KeystorePasswordFromFile() throws Exception {
        assumeTrue("only for systemd", Platforms.isSystemd() && distribution().isPackage());
        String password = "!@#$%^&*()|\\<>/?";
        Path esKeystorePassphraseFile = installation.config.resolve("eks");

        rmKeystoreIfExists();
        createKeystore(password);

        assertPasswordProtectedKeystore();

        try {
            sh.run("sudo systemctl set-environment ES_KEYSTORE_PASSPHRASE_FILE=" + esKeystorePassphraseFile);

            Files.createFile(esKeystorePassphraseFile);
            Files.write(esKeystorePassphraseFile, singletonList(password));

            startRenameme();
            ServerUtils.runRenamemeTests();
            stopRenameme();
        } finally {
            sh.run("sudo systemctl unset-environment ES_KEYSTORE_PASSPHRASE_FILE");
        }
    }

    public void test51WrongKeystorePasswordFromFile() throws Exception {
        assumeTrue("only for systemd", Platforms.isSystemd() && distribution().isPackage());
        Path esKeystorePassphraseFile = installation.config.resolve("eks");

        assertPasswordProtectedKeystore();

        try {
            sh.run("sudo systemctl set-environment ES_KEYSTORE_PASSPHRASE_FILE=" + esKeystorePassphraseFile);

            if (Files.exists(esKeystorePassphraseFile)) {
                FileUtils.rm(esKeystorePassphraseFile);
            }

            Files.createFile(esKeystorePassphraseFile);
            Files.write(esKeystorePassphraseFile, singletonList("wrongpassword"));

            Packages.JournaldWrapper journaldWrapper = new Packages.JournaldWrapper(sh);
            Shell.Result result = runRenamemeStartCommand(null, false, false);
            assertRenamemeFailure(result, Arrays.asList(ERROR_INCORRECT_PASSWORD, ERROR_CORRUPTED_KEYSTORE), journaldWrapper);
        } finally {
            sh.run("sudo systemctl unset-environment ES_KEYSTORE_PASSPHRASE_FILE");
        }
    }

    /**
     * Check that we can mount a password-protected keystore to a docker image
     * and provide a password via an environment variable.
     */
    public void test60DockerEnvironmentVariablePassword() throws Exception {
        assumeTrue(distribution().isDocker());
        String password = "password";
        Path dockerKeystore = installation.config("renameme.keystore");

        Path localKeystoreFile = getKeystoreFileFromDockerContainer(password, dockerKeystore);

        // restart ES with password and mounted keystore
        Map<Path, Path> volumes = new HashMap<>();
        volumes.put(localKeystoreFile, dockerKeystore);
        Map<String, String> envVars = new HashMap<>();
        envVars.put("KEYSTORE_PASSWORD", password);
        Docker.runContainer(distribution(), volumes, envVars);
        Docker.waitForRenameme(installation);
        ServerUtils.runRenamemeTests();
    }

    /**
     * Check that we can mount a password-protected keystore to a docker image
     * and provide a password via a file, pointed at from an environment variable.
     */
    public void test61DockerEnvironmentVariablePasswordFromFile() throws Exception {
        assumeTrue(distribution().isDocker());

        Path tempDir = null;
        try {
            tempDir = createTempDir(DockerTests.class.getSimpleName());

            String password = "password";
            String passwordFilename = "password.txt";
            Files.write(tempDir.resolve(passwordFilename), singletonList(password));
            Files.setPosixFilePermissions(tempDir.resolve(passwordFilename), FileMatcher.p600);

            Path dockerKeystore = installation.config("renameme.keystore");

            Path localKeystoreFile = getKeystoreFileFromDockerContainer(password, dockerKeystore);

            // restart ES with password and mounted keystore
            Map<Path, Path> volumes = new HashMap<>();
            volumes.put(localKeystoreFile, dockerKeystore);
            volumes.put(tempDir, Paths.get("/run/secrets"));

            Map<String, String> envVars = new HashMap<>();
            envVars.put("KEYSTORE_PASSWORD_FILE", "/run/secrets/" + passwordFilename);

            Docker.runContainer(distribution(), volumes, envVars);

            Docker.waitForRenameme(installation);
            ServerUtils.runRenamemeTests();
        } finally {
            if (tempDir != null) {
                FileUtils.rm(tempDir);
            }
        }
    }

    /**
     * Check that if we provide the wrong password for a mounted and password-protected
     * keystore, Renameme doesn't start.
     */
    public void test62DockerEnvironmentVariableBadPassword() throws Exception {
        assumeTrue(distribution().isDocker());
        String password = "password";
        Path dockerKeystore = installation.config("renameme.keystore");

        Path localKeystoreFile = getKeystoreFileFromDockerContainer(password, dockerKeystore);

        // restart ES with password and mounted keystore
        Map<Path, Path> volumes = new HashMap<>();
        volumes.put(localKeystoreFile, dockerKeystore);
        Map<String, String> envVars = new HashMap<>();
        envVars.put("KEYSTORE_PASSWORD", "wrong");
        Shell.Result r = Docker.runContainerExpectingFailure(distribution(), volumes, envVars);
        assertThat(r.stderr, containsString(ERROR_INCORRECT_PASSWORD));
    }

    /**
     * In the Docker context, it's a little bit tricky to get a password-protected
     * keystore. All of the utilities we'd want to use are on the Docker image.
     * This method mounts a temporary directory to a Docker container, password-protects
     * the keystore, and then returns the path of the file that appears in the
     * mounted directory (now accessible from the local filesystem).
     */
    private Path getKeystoreFileFromDockerContainer(String password, Path dockerKeystore) throws IOException {
        // Mount a temporary directory for copying the keystore
        Path dockerTemp = Paths.get("/usr/tmp/keystore-tmp");
        Path tempDirectory = createTempDir(KeystoreManagementTests.class.getSimpleName());
        Map<Path, Path> volumes = new HashMap<>();
        volumes.put(tempDirectory, dockerTemp);

        // It's very tricky to properly quote a pipeline that you're passing to
        // a docker exec command, so we're just going to put a small script in the
        // temp folder.
        List<String> setPasswordScript = new ArrayList<>();
        setPasswordScript.add("echo \"" + password);
        setPasswordScript.add(password);
        setPasswordScript.add("\" | " + installation.executables().keystoreTool.toString() + " passwd");

        Files.write(tempDirectory.resolve("set-pass.sh"), setPasswordScript);

        Docker.runContainer(distribution(), volumes, null);
        try {
            Docker.waitForPathToExist(dockerTemp);
            Docker.waitForPathToExist(dockerKeystore);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // We need a local shell to put the correct permissions on our mounted directory.
        Shell localShell = new Shell();
        localShell.run("docker exec --tty " + Docker.getContainerId() + " chown renameme:root " + dockerTemp);
        localShell.run("docker exec --tty " + Docker.getContainerId() + " chown renameme:root " + dockerTemp.resolve("set-pass.sh"));

        sh.run("bash " + dockerTemp.resolve("set-pass.sh"));

        // copy keystore to temp file to make it available to docker host
        sh.run("cp " + dockerKeystore + " " + dockerTemp);
        return tempDirectory.resolve("renameme.keystore");
    }

    /** Create a keystore. Provide a password to password-protect it, otherwise use null */
    private void createKeystore(String password) throws Exception {
        Path keystore = installation.config("renameme.keystore");
        final Installation.Executables bin = installation.executables();
        bin.keystoreTool.run("create");

        // this is a hack around the fact that we can't run a command in the same session as the same user but not as administrator.
        // the keystore ends up being owned by the Administrators group, so we manually set it to be owned by the vagrant user here.
        // from the server's perspective the permissions aren't really different, this is just to reflect what we'd expect in the tests.
        // when we run these commands as a role user we won't have to do this
        Platforms.onWindows(() -> sh.chown(keystore));

        if (distribution().isDocker()) {
            try {
                Docker.waitForPathToExist(keystore);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (password != null) {
            setKeystorePassword(password);
        }
    }

    private void rmKeystoreIfExists() {
        Path keystore = installation.config("renameme.keystore");
        if (distribution().isDocker()) {
            try {
                Docker.waitForPathToExist(keystore);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Move the auto-created one out of the way, or else the CLI prompts asks us to confirm
            sh.run("rm " + keystore);
        } else {
            if (Files.exists(keystore)) {
                FileUtils.rm(keystore);
            }
        }
    }

    private void setKeystorePassword(String password) throws Exception {
        final Installation.Executables bin = installation.executables();

        // set the password by passing it to stdin twice
        Platforms.onLinux(() -> bin.keystoreTool.run("passwd", password + "\n" + password + "\n"));

        Platforms.onWindows(
            () -> sh.run("Invoke-Command -ScriptBlock {echo '" + password + "'; echo '" + password + "'} | " + bin.keystoreTool + " passwd")
        );
    }

    private void assertPasswordProtectedKeystore() {
        Shell.Result r = installation.executables().keystoreTool.run("has-passwd");
        assertThat("keystore should be password protected", r.exitCode, is(0));
    }

    private void verifyKeystorePermissions() {
        Path keystore = installation.config("renameme.keystore");
        switch (distribution.packaging) {
            case Distribution.Packaging.TAR:
            case Distribution.Packaging.ZIP:
                assertThat(
                    keystore,
                    FileMatcher.file(FileMatcher.Fileness.File, Archives.ARCHIVE_OWNER, Archives.ARCHIVE_OWNER, FileMatcher.p660)
                );
                break;
            case Distribution.Packaging.DEB:
            case Distribution.Packaging.RPM:
                assertThat(keystore, FileMatcher.file(FileMatcher.Fileness.File, "root", "renameme", FileMatcher.p660));
                break;
            case Distribution.Packaging.DOCKER:
            case Distribution.Packaging.DOCKER_UBI:
                Docker.assertPermissionsAndOwnership(keystore, FileMatcher.p660);
                break;
            default:
                throw new IllegalStateException("Unknown Renameme packaging type.");
        }
    }
}
