/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author claas
 */
@Configuration
public class TomcatConfiguration {

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        final int ajpPort = 8009;

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        Connector ajpConnector = new Connector("AJP/1.3");
        ajpConnector.setPort(ajpPort);
        ajpConnector.setSecure(false);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme("http");

        AbstractAjpProtocol ajpProtocol = (AbstractAjpProtocol) ajpConnector.getProtocolHandler();
        ajpProtocol.setSecretRequired(false);

        tomcat.addAdditionalTomcatConnectors(ajpConnector);

        return tomcat;
    }
}
