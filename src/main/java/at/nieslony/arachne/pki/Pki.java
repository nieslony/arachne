/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import at.nieslony.arachne.setup.SetupData;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class Pki {

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private KeyRepository keyRepository;

    private static final Logger logger = LoggerFactory.getLogger(Pki.class);

    private KeyPairGenerator keyPairGenerator;

    private final static String SK_PREFIX = "pki";
    private final static String SK_DH_PARAMS = SK_PREFIX + ".dhParams";

    public enum CertSpecKey {
        SK_KEY_ALGO("keyAlgo"),
        SK_KEY_SIZE("keySize"),
        SK_CERT_LIFETIME_DAYS("certLifeTimeDays"),
        SK_SUBJECT("subject"),
        SK_SIGNATURE_ALGO("signatureAlgo");

        private final String key;

        CertSpecKey(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum CertSpecType {
        CA_SPEC("caSpec"),
        SERVER_SPEC("serverSpec"),
        USER_SPEC("userSpec");

        private final String type;

        CertSpecType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    private static String setting(CertSpecType certSpecType, CertSpecKey field) {
        return SK_PREFIX + "." + certSpecType + "." + field;
    }

    private PrivateKey rootKey = null;
    private X509Certificate rootCert = null;

    public void fromSetupData(SetupData setupData) throws PkiSetupException {
        logger.info("Verify and save PKI settings");
        saveCertSpecs(setupData.getCaCertSpecs(), CertSpecType.CA_SPEC);
        saveCertSpecs(setupData.getServerCertSpecs(), CertSpecType.SERVER_SPEC);
        saveCertSpecs(setupData.getUserCertSpecs(), CertSpecType.USER_SPEC);

        getRootCert();
        try {
            getServerCert();
        } catch (PkiNotInitializedException ex) {
            throw new PkiSetupException(ex);
        }
        generateDhParams(2048);
    }

    private void saveCertSpecs(CertSpecs certSpecs, CertSpecType specsType) throws PkiSetupException {
        if (certSpecs == null) {
            throw new PkiSetupException(specsType, "not provided");
        }

        KeyPairGenerator testKeyPairGenerator;
        if (certSpecs.getKeyAlgo() == null) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_KEY_ALGO, "not provided");
        }
        try {
            testKeyPairGenerator = KeyPairGenerator.getInstance(certSpecs.getKeyAlgo());
        } catch (NoSuchAlgorithmException ex) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_KEY_ALGO, ex.getMessage());
        }
        KeyPair testKey = testKeyPairGenerator.generateKeyPair();

        if (certSpecs.getKeySize() == 0) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_KEY_SIZE, "must be > 0");
        }
        try {
            testKeyPairGenerator.initialize(certSpecs.getKeySize());
        } catch (InvalidParameterException ex) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_KEY_SIZE, ex.getMessage());
        }

        if (certSpecs.getCertLifeTimeDays() == 0) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_CERT_LIFETIME_DAYS, "must be > 0");
        }

        X500Name subject;
        if (certSpecs.getSubject() == null) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_SUBJECT, "not provided");
        }
        try {
            subject = new X500Name(certSpecs.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_SUBJECT, ex.getMessage());
        }

        if (certSpecs.getSignatureAlgo() == null) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_SIGNATURE_ALGO, "not provided");
        }
        try {
            new JcaContentSignerBuilder(certSpecs.getSignatureAlgo())
                    .build(testKey.getPrivate());
        } catch (OperatorCreationException | IllegalArgumentException ex) {
            throw new PkiSetupException(specsType, CertSpecKey.SK_SIGNATURE_ALGO, ex.getMessage());
        }

        settingsRepository.save(new SettingsModel(
                setting(specsType, CertSpecKey.SK_KEY_ALGO),
                certSpecs.getKeyAlgo()));
        settingsRepository.save(new SettingsModel(
                setting(specsType, CertSpecKey.SK_KEY_SIZE),
                certSpecs.getKeySize()));
        settingsRepository.save(new SettingsModel(
                setting(specsType, CertSpecKey.SK_CERT_LIFETIME_DAYS),
                certSpecs.getCertLifeTimeDays()));
        settingsRepository.save(new SettingsModel(
                setting(specsType, CertSpecKey.SK_SUBJECT),
                subject.toString()));
        settingsRepository.save(new SettingsModel(
                setting(specsType, CertSpecKey.SK_SIGNATURE_ALGO),
                certSpecs.getSignatureAlgo()));
    }

    public String getRootCertAsBase64() {
        StringWriter sw = new StringWriter();

        try (JcaPEMWriter jpw = new JcaPEMWriter(sw)) {
            jpw.writeObject(getRootCert());
        } catch (IOException ex) {
            String msg = "Cannot write CA certificate: " + ex.getMessage();
            logger.error(msg);
            return msg;
        }

        return sw.toString();
    }

    private String asBase64(Object obj) {
        StringWriter sw = new StringWriter();

        try (JcaPEMWriter jpw = new JcaPEMWriter(sw)) {
            jpw.writeObject(obj);
        } catch (IOException ex) {
            String msg = "Cannot write private key: " + ex.getMessage();
            logger.error(msg);
            return msg;
        }

        return sw.toString();
    }

    public String getRootKeyAsBase64() {
        return asBase64(rootCert);
    }

    public X509Certificate getRootCert() {
        if (rootCert == null) {
            String rootCertSubject
                    = settingsRepository
                            .findBySetting(setting(
                                    CertSpecType.CA_SPEC,
                                    CertSpecKey.SK_SUBJECT)
                            )
                            .get()
                            .getContent();

            List<CertificateModel> certModList = certificateRepository.findBySubjectAndCertType(
                    rootCertSubject,
                    CertificateModel.CertType.CA
            );

            if (certModList.isEmpty()) {
                logger.info("Creating root certificate: " + rootCertSubject);
                createRootCert();
                KeyModel keyModel = keyRepository.save(new KeyModel(rootKey));
                certificateRepository.save(new CertificateModel(
                        rootCert,
                        CertificateModel.CertType.CA,
                        keyModel));
            } else {
                logger.info("Loading root certitificate from DB");
                rootCert = certModList.get(0).getCertificate();
                rootKey = certModList.get(0).getKeyModel().getPrivateKey();
            }
        }

        return rootCert;
    }

    private void createRootCert() {
        try {
            String keyAlgo = settingsRepository
                    .findBySetting(setting(CertSpecType.CA_SPEC, CertSpecKey.SK_KEY_ALGO))
                    .orElseThrow(() -> new PkiNotInitializedException("Cannot find root cert keyAlgo"))
                    .getContent();
            int keySize = Integer.parseInt(settingsRepository
                    .findBySetting(setting(CertSpecType.CA_SPEC, CertSpecKey.SK_KEY_SIZE))
                    .orElseThrow(() -> new PkiNotInitializedException("Cannot find root cert key size"))
                    .getContent());
            int caLifetimeDays = Integer.parseInt(settingsRepository
                    .findBySetting(setting(CertSpecType.CA_SPEC, CertSpecKey.SK_CERT_LIFETIME_DAYS))
                    .orElseThrow(() -> new PkiNotInitializedException("Cannot find root cert lifetime"))
                    .getContent());
            String caSubject = settingsRepository
                    .findBySetting(setting(CertSpecType.CA_SPEC, CertSpecKey.SK_SUBJECT))
                    .orElseThrow(() -> new PkiNotInitializedException("Cannot find root cert subject"))
                    .getContent();
            String caSignatureAlgo = settingsRepository
                    .findBySetting(setting(CertSpecType.CA_SPEC, CertSpecKey.SK_SIGNATURE_ALGO))
                    .orElseThrow(() -> new PkiNotInitializedException("Cannot find root cert signatureAlgo"))
                    .getContent();

            keyPairGenerator = KeyPairGenerator.getInstance(keyAlgo);
            keyPairGenerator.initialize(keySize);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            Date startDate = cal.getTime();

            cal.add(Calendar.DATE, caLifetimeDays);
            Date endDate = cal.getTime();

            KeyPair keyPair = keyPairGenerator.genKeyPair();
            BigInteger rootSerial = new BigInteger(Long.toString(new SecureRandom().nextLong()));

            X500Name rootCertIssuer = new X500Name(caSubject);
            X500Name rootCertSubject = new X500Name(caSubject);
            ContentSigner contentSigner = new JcaContentSignerBuilder(caSignatureAlgo)
                    .build(keyPair.getPrivate());

            X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(
                    rootCertIssuer,
                    rootSerial,
                    startDate,
                    endDate,
                    rootCertSubject,
                    keyPair.getPublic()
            );

            JcaX509ExtensionUtils rootCertExtUtils = new JcaX509ExtensionUtils();
            rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            rootCertBuilder.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    rootCertExtUtils.createSubjectKeyIdentifier(keyPair.getPublic())
            );

            X509CertificateHolder rootCertHolder = rootCertBuilder.build(contentSigner);
            rootCert = new JcaX509CertificateConverter()
                    .getCertificate(rootCertHolder);
            rootKey = keyPair.getPrivate();
        } catch (PkiNotInitializedException | CertIOException | NoSuchAlgorithmException | OperatorCreationException | CertificateException ex) {
            logger.error("Error genarating root certitficate: " + ex.getMessage());
        }
    }

    public X509Certificate getServerCert() throws PkiNotInitializedException {
        return getServerCertModel().getCertificate();
    }

    public PrivateKey getServerKey() throws PkiNotInitializedException {
        CertificateModel cm = getServerCertModel();
        return cm.getKeyModel().getPrivateKey();
    }

    public String getServerKeyAsBase64() throws PkiNotInitializedException {
        return asBase64(getServerKey());
    }

    private CertificateModel getServerCertModel() throws PkiNotInitializedException {
        String subject = settingsRepository
                .findBySetting(setting(CertSpecType.SERVER_SPEC, CertSpecKey.SK_SUBJECT))
                .orElseThrow(() -> new PkiNotInitializedException("Cannot find server cert subject"))
                .getContent();
        List<CertificateModel> certModels
                = certificateRepository.findBySubjectAndCertType(
                        subject,
                        CertificateModel.CertType.SERVER);
        Date now = new Date();
        for (CertificateModel cm : certModels) {
            if (!cm.getIsRevoked() && now.compareTo(cm.getValidTo()) < 0) {
                return cm;
            }
        }

        String keyAlgo = settingsRepository
                .findBySetting(setting(CertSpecType.SERVER_SPEC, CertSpecKey.SK_KEY_ALGO))
                .orElseThrow(() -> new PkiNotInitializedException("Cannot find server cert keyAlgo"))
                .getContent();
        int keySize = Integer.parseInt(settingsRepository
                .findBySetting(setting(CertSpecType.SERVER_SPEC, CertSpecKey.SK_KEY_SIZE))
                .orElseThrow(() -> new PkiNotInitializedException("Cannot find server cert keySize"))
                .getContent());
        int lifetimeDays = Integer.parseInt(settingsRepository
                .findBySetting(setting(CertSpecType.SERVER_SPEC, CertSpecKey.SK_CERT_LIFETIME_DAYS))
                .orElseThrow(() -> new PkiNotInitializedException("Cannot find server cert lifetimeDays"))
                .getContent());
        String signatureAlgo = settingsRepository
                .findBySetting(setting(CertSpecType.SERVER_SPEC, CertSpecKey.SK_SIGNATURE_ALGO))
                .orElseThrow(() -> new PkiNotInitializedException("Cannot find server cert signatureAlgo"))
                .getContent();
        logger.info("Creating server certificate: " + subject);
        CertificateModel certModel = createCertificate(
                keyAlgo,
                keySize,
                lifetimeDays,
                subject,
                signatureAlgo);

        return certModel;
    }

    public String getServerCertAsBase64() {
        try {
            X509Certificate cert = getServerCert();
            return asBase64(cert);
        } catch (PkiNotInitializedException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    public X509Certificate createUserCert(String username) {
        return null;
    }

    private CertificateModel createCertificate(
            String keyAlgo,
            int keySize,
            int lifeTimeDays,
            String subjectStr,
            String signiungAlgo)
            throws PkiNotInitializedException {
        KeyPair keyPair;
        X509Certificate cert;
        try {
            X500Name rootSubject = new X500Name(
                    settingsRepository
                            .findBySetting(setting(CertSpecType.CA_SPEC, CertSpecKey.SK_SUBJECT))
                            .orElseThrow(() -> new PkiNotInitializedException("Cannot find root cert subject"))
                            .getContent()
            );

            X500Name subject = new X500Name(subjectStr);
            BigInteger serial = new BigInteger(Long.toString(new SecureRandom().nextLong()));
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgo);
            keyPairGenerator.initialize(keySize);
            keyPair = keyPairGenerator.generateKeyPair();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            Date startDate = calendar.getTime();
            calendar.add(Calendar.DATE, lifeTimeDays);
            Date endData = calendar.getTime();

            PKCS10CertificationRequestBuilder p10Builder
                    = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
            JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(signiungAlgo);

            ContentSigner csrContentSigner = csrBuilder.build(rootKey);
            PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    rootSubject,
                    serial,
                    startDate,
                    endData,
                    csr.getSubject(),
                    csr.getSubjectPublicKeyInfo()
            );

            JcaX509ExtensionUtils certUtils = new JcaX509ExtensionUtils();
            certBuilder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false));
            certBuilder.addExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    certUtils.createAuthorityKeyIdentifier(rootCert)
            );
            certBuilder.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    certUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())
            );
            certBuilder.addExtension(
                    Extension.keyUsage,
                    false,
                    new KeyUsage(KeyUsage.keyEncipherment)
            );

            X509CertificateHolder certHolder = certBuilder.build(csrContentSigner);
            cert = new JcaX509CertificateConverter()
                    .getCertificate(certHolder);

        } catch (OperatorCreationException | CertificateException | CertIOException | NoSuchAlgorithmException ex) {
            logger.error(
                    "Cannot create certificate (%s): %s"
                            .formatted(subjectStr, ex.getMessage())
            );
            return null;
        }

        KeyModel keyModel = keyRepository.save(new KeyModel(keyPair.getPrivate()));
        CertificateModel certModel = certificateRepository.save(new CertificateModel(
                cert,
                CertificateModel.CertType.SERVER,
                keyModel
        ));

        return certModel;
    }

    public void generateDhParams(int bits) {
        logger.info("Generating DH params");
        DHParametersGenerator dhGen = new DHParametersGenerator();
        try {
            dhGen.init(bits, 80, SecureRandom.getInstance("NativePRNG"));
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Cannot create securerandom: " + ex.getMessage());
            return;
        }
        DHParameters params = dhGen.generateParameters();
        DHParameters realParams = new DHParameters(params.getP(), BigInteger.valueOf(2));
        ASN1EncodableVector sequence = new ASN1EncodableVector();
        sequence.add(new ASN1Integer(realParams.getP()));
        sequence.add(new ASN1Integer(realParams.getG()));
        byte[] derEncoded;

        try {
            derEncoded = new DERSequence(sequence).getEncoded();
            PemObject pemObject = new PemObject("DH PARAMETERS", derEncoded);
            StringWriter sw = new StringWriter();
            PemWriter pw = new PemWriter(sw);
            pw.writeObject(pemObject);
            pw.close();
            settingsRepository.save(new SettingsModel(SK_DH_PARAMS, sw.toString()));
        } catch (IOException ex) {
            logger.error("Error generating dh params: " + ex.getMessage());
        }
    }

    public String getDhParams() throws PkiNotInitializedException {
        return settingsRepository
                .findBySetting(SK_DH_PARAMS)
                .orElseThrow(() -> new PkiNotInitializedException("Cannot find DH params"))
                .getContent();
    }
}
