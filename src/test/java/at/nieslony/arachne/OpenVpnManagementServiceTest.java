/*
 * Copyright (C) 2026 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.management.ManagementException;
import at.nieslony.arachne.openvpn.management.OpenVpnManagementIf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author claas
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenVpnManagementServiceTest {

    OpenVpnManagementIf mgmtIf;

    private Process openVpnServerProc;
    private Process openVpnClientProc;

    private static final String FN_TMP_DIR = "/tmp/openvpn-test/";
    private static final String FN_CA_CERT = FN_TMP_DIR + "ca.crt";
    private static final String FN_CA_KEY = FN_TMP_DIR + "ca.ckey";
    private static final String FN_SRV_CERT = FN_TMP_DIR + "srv.crt";
    private static final String FN_SRV_KEY = FN_TMP_DIR + "srv.key";
    private static final String FN_CLIENT_CERT = FN_TMP_DIR + "client.crt";
    private static final String FN_CLIENT_KEY = FN_TMP_DIR + "client.key";
    private static final String FN_DH_PARAMS = FN_TMP_DIR + "dh.pem";
    private static final String FN_MANAGEMENT_SOCKET = FN_TMP_DIR + "management.sock";
    private static final String KEY_PWD = "1234";

    private void createCaCert(String fnCert, String fnKey)
            throws IOException, InterruptedException {
        System.out.println("Creating " + fnCert);
        Process proc = new ProcessBuilder(
                "/usr/bin/openssl",
                "req", "-x509",
                "-new",
                "-nodes",
                "-newkey", "rsa:2048",
                "-keyout", fnKey,
                "-out", fnCert,
                "-subj", "/CN=Test_CA"
        ).inheritIO().start();
        proc.waitFor();
    }

    private void createCert(String cn, String fnCert, String fnKey)
            throws IOException, InterruptedException {
        Process proc = new ProcessBuilder(
                "/usr/bin/openssl",
                "req", "-x509",
                "-new",
                "-newkey", "rsa:2048",
                "-keyout", fnKey,
                "-out", fnCert,
                "-CA", FN_CA_CERT,
                "-CAkey", FN_CA_KEY,
                "-subj", "/CN=" + cn,
                "-passout", "pass:" + KEY_PWD
        ).inheritIO().start();
        proc.waitFor();

        proc = new ProcessBuilder(
                "/usr/bin/openssl",
                "rsa",
                "-in", fnKey,
                "-out", fnKey,
                "-passin", "pass:" + KEY_PWD
        ).inheritIO().start();
        proc.waitFor();
    }

    private void createDhParams() throws IOException, InterruptedException {
        System.out.println("Creating DH params");
        Process proc = new ProcessBuilder(
                "/usr/bin/openssl",
                "dhparam",
                "-dsaparam",
                "-out", FN_DH_PARAMS,
                "2048"
        ).inheritIO().start();
        proc.waitFor();
    }

    @BeforeAll
    public void createCertificates() throws IOException, InterruptedException {
        Files.createDirectories(Path.of(FN_TMP_DIR));
        if (!Files.exists(Path.of(FN_CA_CERT))) {
            createCaCert(FN_CA_CERT, FN_CA_KEY);
        }
        if (!Files.exists(Path.of(FN_SRV_CERT))) {
            createCert("www.example.com", FN_SRV_CERT, FN_SRV_KEY);
        }
        if (!Files.exists(Path.of(FN_CLIENT_CERT))) {
            createCert("test-client", FN_CLIENT_CERT, FN_CLIENT_KEY);
        }
        if (!Files.exists(Path.of(FN_DH_PARAMS))) {
            createDhParams();
        }
        System.out.println("All Files created");
    }

    @BeforeAll
    public void startOpenVpnServer() throws IOException, InterruptedException {
        var cmdLine = List.of(
                "/usr/bin/sudo", "--non-interactive",
                "/usr/bin/openvpn",
                "--server", "192.168.1.0", "255.255.255.0",
                "--local", "0.0.0.0",
                "--proto", "tcp-server",
                "--port", "1194",
                "--dev", "tun",
                "--topology", "subnet",
                "--dh", FN_DH_PARAMS,
                "--ca", FN_CA_CERT,
                "--cert", FN_SRV_CERT,
                "--key", FN_SRV_KEY,
                "--management", FN_MANAGEMENT_SOCKET, "unix",
                "--management-client-user", System.getProperty("user.name")
        );
        System.out.println("Starting " + String.join(" ", cmdLine));
        openVpnServerProc = new ProcessBuilder(cmdLine)
                .inheritIO()
                .start();
    }

    @BeforeAll
    public void startOpenVpnClient() throws IOException, InterruptedException {
        var cmdLine = List.of(
                "/usr/bin/sudo", "--non-interactive",
                "/usr/bin/openvpn",
                "--client",
                "--dev", "tun",
                "--remote", "localhost", "1194", "tcp",
                "--ca", FN_CA_CERT,
                "--cert", FN_CLIENT_CERT,
                "--key", FN_CLIENT_KEY
        );
        System.out.println("Starting " + String.join(" ", cmdLine));
        openVpnClientProc = new ProcessBuilder(cmdLine)
                .inheritIO()
                .start();
    }

    @BeforeAll
    public void startManagementIf() throws InterruptedException {
        mgmtIf = new OpenVpnManagementIf(
                () -> {
                },
                Path.of(FN_MANAGEMENT_SOCKET),
                "T"
        );
        mgmtIf.run();
        Thread.sleep(10 * 1000);
    }

    @Test
    @Order(1)
    public void testVersion() throws ManagementException {
        System.out.println("Version: " + mgmtIf.version());
    }

    @Test
    @Order(2)
    public void testPid1() throws ManagementException {
        System.out.println("Pid: " + mgmtIf.pid());
    }

    @Test
    @Order(3)
    public void testRestart() throws ManagementException {
        //mgmtIf.restartServer();
        System.out.println(mgmtIf.status());
    }

    @Test
    @Order(6)
    public void testPid2() throws ManagementException {
        System.out.println("Pid: " + mgmtIf.pid());
    }

    @AfterAll
    public void stopOpenVpnServer() {
        System.out.println("Shutting down OpenVpn server");
        openVpnClientProc.destroy();
        openVpnServerProc.destroy();
    }

}
