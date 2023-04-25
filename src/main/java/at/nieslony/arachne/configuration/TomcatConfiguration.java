/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

import at.nieslony.arachne.tomcat.TomcatSettings;
import at.nieslony.arachne.settings.Settings;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author claas
 */
@Configuration
public class TomcatConfiguration {

    @Autowired
    Settings settings;

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        final int ajpPort = 8009;

        TomcatSettings tomcatSettings = new TomcatSettings(settings);
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        if (tomcatSettings.isEnableAjpConnector()) {
            Connector ajpConnector = new Connector("AJP/1.3");
            ajpConnector.setPort(tomcatSettings.getAjpPort());
            ajpConnector.setSecure(false);
            ajpConnector.setAllowTrace(false);
            ajpConnector.setScheme("http");

            AbstractAjpProtocol ajpProtocol = (AbstractAjpProtocol) ajpConnector.getProtocolHandler();
            ajpProtocol.setSecretRequired(tomcatSettings.isEnableAjpSecret());
            if (tomcatSettings.isEnableAjpSecret()) {
                ajpProtocol.setSecret(tomcatSettings.getAjpSecret());
            }

            tomcat.addAdditionalTomcatConnectors(ajpConnector);
        }

        return tomcat;
    }
}
