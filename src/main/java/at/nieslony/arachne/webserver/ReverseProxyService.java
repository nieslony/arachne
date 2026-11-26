/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.webserver;

import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.net.NetUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
@Slf4j
public class ReverseProxyService {

    public static String indentString(String str, int indent) {
        return Arrays.stream(str.split("\n"))
                .map(s -> " ".repeat(indent) + s)
                .collect(Collectors.joining("\n"));
    }

    private final String FN_ARACHNE_CONF = "arachne.conf";
    private final String FN_ARACHNE_INT_CONF = "arachne-internal.conf";
    private final String FN_ARACHNE_EXT_CONF = "arachne-external.conf";

    @Value("${arachneConfigDir}")
    private String arachneConfigDir;

    @Autowired
    private Settings settings;

    public String getApacheConfigFileName() {
        String fn = arachneConfigDir + "/" + FN_ARACHNE_CONF;
        try {
            return new File(fn).getCanonicalPath();
        } catch (IOException ex) {
            log.warn("Cannot get canonical name of %s: %s"
                    .formatted(fn, ex.getMessage())
            );
            return fn;
        }
    }

    public String getApacheIntConfigFileName() {
        String fn = arachneConfigDir + "/" + FN_ARACHNE_INT_CONF;
        try {
            return new File(fn).getCanonicalPath();
        } catch (IOException ex) {
            log.warn("Cannot get canonical name of %s: %s"
                    .formatted(fn, ex.getMessage())
            );
            return fn;
        }
    }

    public String getApacheExtConfigFileName() {
        String fn = arachneConfigDir + "/" + FN_ARACHNE_EXT_CONF;
        try {
            return new File(fn).getCanonicalPath();
        } catch (IOException ex) {
            log.warn("Cannot get canonical name of %s: %s"
                    .formatted(fn, ex.getMessage())
            );
            return fn;
        }
    }

    private void writeProxyPath(
            PrintWriter writer,
            TomcatSettings tomcatSettings,
            ReverseProxySettings reverseProxySettings
    ) {
        if (tomcatSettings.isEnableAjpConnector()) {
            writer.print("ProxyPass \"%s\" \"ajp://127.0.0.1:%d/arachne\""
                    .formatted(
                            reverseProxySettings.getProxyPath(),
                            tomcatSettings.getAjpPort()
                    )
            );
            if (tomcatSettings.isEnableAjpSecret()) {
                writer.write(" secret=" + tomcatSettings.getAjpSecret());
            }
            writer.println();
        } else if (tomcatSettings.isHttpsConnectorEnabled()) {
            writer.println("ProxyPass \"%s\" \"https://%s:%d/arachne\""
                    .formatted(
                            reverseProxySettings.getProxyPath(),
                            NetUtils.myHostname(),
                            tomcatSettings.getHttpsPort()
                    )
            );
        } else {
            writer.println("ProxyPass \"%s\" \"https://%s:8080/arachne\""
                    .formatted(
                            reverseProxySettings.getProxyPath(),
                            NetUtils.myHostname()
                    )
            );
        }

        if (!reverseProxySettings.getProxyPath().equals("/arachne")) {
            writer.write("ProxyPassReverse \"%s\" \"https://%s/arachne\"\n"
                    .formatted(
                            reverseProxySettings.getProxyPath(),
                            NetUtils.myHostname()
                    )
            );
        }
    }

    private void writeExternalLocation(
            PrintWriter writer,
            ReverseProxySettings reverseProxySettings
    ) {
        writer.println("""

                    <Location %s/otv>
                        Require all granted
                    </Location>
                    """.formatted(reverseProxySettings.getProxyPath())
        );
    }

    private void writeInternalLocation(
            PrintWriter writer,
            PreAuthSettings preAuthSettings,
            ReverseProxySettings reverseProxySettings
    ) {
        String requirePermissions
                = """
                <RequireAny>
                    Require ip 10.0.0.0/8
                    Require ip 172.16.0.0/12
                    Require ip 192.168.0.0/16
                </RequireAny>
                """;

        writer.println("<Location %s>"
                .formatted(reverseProxySettings.getProxyPath())
        );
        if (preAuthSettings.isPreAuthtEnabled()) {
            if (reverseProxySettings.getAddAuthentication() == ReverseProxySettings.AddAuthentication.KERBEROS) {
                writer.println("""
                        AuthType            GSSAPI
                        AuthName            "Arachne"
                        GssapiCredStore     keytab:%s
                        GssapiBasicAuth     On
                        GssapiUseSessions   On
                        Session             On
                        SessionCookieName   gssapi_session path=/arachne;httponly;secure;
                    """.formatted(reverseProxySettings.getKeytabFile())
                );
                requirePermissions
                        = """
                        <RequireAll>
                            Require valid-user
                        %s
                        </RequireAll>
                        """.formatted(indentString(requirePermissions, 4));
            }
            writer.println(switch (preAuthSettings.getPreAuthSource()) {
                case ENVIRONMENT_VARIABLE ->
                    """
                        RewriteEngine       on
                        RewriteRule         ^(.*) $1 [E=AJP_%1$s:%%{%1$s},UnsafeAllow3F]
                        RewriteCond         %%{ENV:%1$s} !^$
                    """.formatted(preAuthSettings.getEnvironmentVariable());
                case HTTP_HEADER ->
                    """
                        RewriteCond         %{LA-U:REMOTE_USER} (.+)
                        RewriteRule         . - [E=RU:%%1]
                        RequestHeader       set %s %%{RU}e
                    """.formatted(preAuthSettings.getHttpHeader());
            });
        }
        writer.println(indentString(requirePermissions, 4));
        writer.println("</Location>");
    }

    public void saveApacheConfig() {
        TomcatSettings tomcatSettings = settings.getSettings(TomcatSettings.class);
        PreAuthSettings preAuthSettings = settings.getSettings(PreAuthSettings.class);
        ReverseProxySettings reverseProxySettings = settings.getSettings(ReverseProxySettings.class);

        String now = new Date().toString();
        if (reverseProxySettings.isSplitIntExtServices()) {
            StringWriter intConfigStr = new StringWriter();
            PrintWriter intConfig = new PrintWriter(intConfigStr);
            intConfig.println("# Generated by Arachne on %s".formatted(now));
            intConfig.println();
            writeProxyPath(intConfig, tomcatSettings, reverseProxySettings);
            writeInternalLocation(intConfig, preAuthSettings, reverseProxySettings);

            String filename;
            filename = getApacheIntConfigFileName();
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(intConfigStr.toString());
            } catch (IOException ex) {
                log.error("Cannot write %s: %s".formatted(filename));
                ShowNotification.error(
                        "Cannot write " + filename,
                        ex.getMessage()
                );
            }

            StringWriter extConfigStr = new StringWriter();
            PrintWriter extConfig = new PrintWriter(extConfigStr);
            extConfig.println("# Generated by Arachne on %s".formatted(now));
            extConfig.println();
            writeProxyPath(extConfig, tomcatSettings, reverseProxySettings);
            writeExternalLocation(extConfig, reverseProxySettings);

            filename = getApacheExtConfigFileName();
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(extConfigStr.toString());
            } catch (IOException ex) {
                log.error("Cannot write %s: %s".formatted(filename));
                ShowNotification.error(
                        "Cannot write " + filename,
                        ex.getMessage()
                );
            }

        } else {
            StringWriter configStr = new StringWriter();
            PrintWriter config = new PrintWriter(configStr);
            config.println("# Generated by Arachne on %s".formatted(now));
            config.println();

            writeProxyPath(config, tomcatSettings, reverseProxySettings);
            writeExternalLocation(config, reverseProxySettings);
            writeInternalLocation(config, preAuthSettings, reverseProxySettings);

            try (FileWriter writer = new FileWriter(getApacheConfigFileName())) {
                writer.write(configStr.toString());
            } catch (IOException ex) {
                log.error("Cannot write %s: %s".formatted(getApacheConfigFileName(), ex.getMessage()));
                ShowNotification.error(
                        "Cannot write " + getApacheConfigFileName(),
                        ex.getMessage()
                );
            }
        }
    }
}
