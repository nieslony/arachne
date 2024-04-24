/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.tomcat.TomcatSettings;
import at.nieslony.arachne.utils.net.NetUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author claas
 */
@Configuration
public class TomcatConfiguration {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(TomcatConfiguration.class);

    @Autowired
    Settings settings;

    @Autowired
    Pki pki;

    @Value("${tomcatCertPath:${arachneConfigDir}/server.crt}")
    String tomcatCertPath;

    @Value("${tomcatKeyPath:${arachneConfigDir}/server.key}")
    String tomcatKeyPath;

    private KeyPair createSslKey() {

        try {
            logger.info("Creating RSA key " + tomcatKeyPath);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    "RSA"
            );
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(
                    new FileWriter(tomcatKeyPath))) {
                pemWriter.writeObject((PrivateKey) keyPair.getPrivate());
            }
            Path path = Paths.get(tomcatKeyPath);

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, perms);

            return keyPair;
        } catch (NoSuchAlgorithmException | IOException ex) {
            logger.error("Cannot write key to %s: %s".formatted(
                    tomcatKeyPath, ex.getMessage()
            ));
            return null;
        }
    }

    private void createSslCertificate(KeyPair keyPair) {
        logger.info("Creating SSL certificate " + tomcatCertPath);
        X509Certificate cert;

        String myHostname = NetUtils.myHostname();
        X500Name subject = new X500Name("CN=" + myHostname);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Instant validFrom = Instant.now();
        Instant validUntil = validFrom.plus(10 * 360, ChronoUnit.DAYS);
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                Date.from(validFrom),
                Date.from(validUntil),
                subject,
                keyPair.getPublic()
        );
        try {
            certBuilder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    new GeneralNames(
                            new GeneralName[]{
                                new GeneralName(GeneralName.dNSName, NetUtils.myDomain()),
                                new GeneralName(GeneralName.dNSName, NetUtils.myHostname()),
                                new GeneralName(GeneralName.dNSName, "localhost")
                            }
                    )
            );
            ContentSigner certSigner = new JcaContentSignerBuilder("SHA256WithRSA")
                    .build(keyPair.getPrivate());
            X509CertificateHolder certHolder = certBuilder.build(certSigner);
            cert = new JcaX509CertificateConverter()
                    .getCertificate(certHolder);
        } catch (CertIOException | OperatorCreationException | CertificateException ex) {
            logger.error("Cannor create certificate: " + ex.getMessage());
            return;
        }
        try (JcaPEMWriter pemWriter
                = new JcaPEMWriter(new FileWriter(tomcatCertPath))) {
            pemWriter.writeObject(cert);
        } catch (IOException ex) {
            logger.error("Cannot write certificate ti %s: %s"
                    .formatted(tomcatCertPath, ex.getMessage())
            );
        }
    }

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatSettings tomcatSettings = settings.getSettings(TomcatSettings.class);
        PreAuthSettings preAuthSettings = settings.getSettings(PreAuthSettings.class);
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        if (tomcatSettings.isHttpConnectorEnabled()) {
            logger.info("Enabling Tomcat SSL");
            if (!Files.exists(Paths.get(tomcatKeyPath))) {
                KeyPair keyPair = createSslKey();
                createSslCertificate(keyPair);
            }

            Ssl ssl = new Ssl();
            ssl.setCertificate(tomcatCertPath);
            ssl.setCertificatePrivateKey(tomcatKeyPath);
            ssl.setEnabled(true);

            tomcat.setSsl(ssl);
            tomcat.setPort(tomcatSettings.getHttpsPort());

            Connector httpConnector = new Connector();
            httpConnector.setPort(8080);
            httpConnector.setRedirectPort(8443);
            tomcat.addAdditionalTomcatConnectors(httpConnector);
        } else {
            logger.info("Tomcat SSL is disabled");
        }

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

            if (preAuthSettings.isPreAuthtEnabled()) {
                ajpProtocol.setTomcatAuthentication(true);
            }
            ajpProtocol.setAllowedRequestAttributesPattern(".*");

            tomcat.addAdditionalTomcatConnectors(ajpConnector);
        }

        return tomcat;
    }
}
