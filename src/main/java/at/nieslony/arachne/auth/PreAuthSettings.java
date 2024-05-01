/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PreAuthSettings extends AbstractSettingsGroup {

    public enum PreAuthSource {
        ENVIRONMENT_VARIABLE("Environment Variable"),
        HTTP_HEADER("HTTP Header");

        private String sourceName;

        PreAuthSource(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public String toString() {
            return sourceName;
        }
    }

    private boolean preAuthtEnabled = false;
    private PreAuthSource preAuthSource = PreAuthSource.ENVIRONMENT_VARIABLE;
    private String environmentVariable = "REMOTE_USER";
    private String httpHeader = "X_REMOTE_USER";

    private boolean writeApachePreAuthConfig = true;
    private String keytabFile = "/etc/httpd/krb5.keytab";
}
