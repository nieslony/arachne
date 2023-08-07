/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.setup.SetupData;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
    private Settings settings;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private KeyRepository keyRepository;

    private static final Logger logger = LoggerFactory.getLogger(Pki.class);

    private KeyPairGenerator keyPairGenerator;

    public Pki() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private PrivateKey rootKey = null;
    private X509Certificate rootCert = null;
    private X509CRL crl = null;

    public void fromSetupData(SetupData setupData) throws CertSpecsValidationException {
        logger.info("Verify and save PKI settings");
        setupData.getCaCertSpecs().save(settings, CertSpecs.CertSpecType.CA_SPEC);
        setupData.getServerCertSpecs().save(settings, CertSpecs.CertSpecType.SERVER_SPEC);
        setupData.getUserCertSpecs().save(settings, CertSpecs.CertSpecType.USER_SPEC);

        getRootCert();
        try {
            getServerCert();
        } catch (PkiException ex) {
            throw new CertSpecsValidationException(ex);
        }
        generateDhParams(2048);
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

    public static String asBase64(Object obj) {
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

    void loadOrCreateRootCert() {
        CertSpecs caCertSpecs = new CertSpecs(settings, CertSpecs.CertSpecType.CA_SPEC);
        String rootCertSubject = caCertSpecs.getSubject();

        List<CertificateModel> certModList
                = certificateRepository.findBySubjectIgnoreCaseAndCertType(
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

    public X509Certificate getRootCert() {
        if (rootCert == null) {
            loadOrCreateRootCert();
        }

        return rootCert;
    }

    public PrivateKey getRootKey() {
        if (rootKey == null) {
            loadOrCreateRootCert();
        }

        return rootKey;
    }

    private void createRootCert() {
        try {
            CertSpecs rootCertSpecs = new CertSpecs(settings, CertSpecs.CertSpecType.CA_SPEC);
            rootCertSpecs.validate();
            String keyAlgo = rootCertSpecs.getKeyAlgo();
            int keySize = rootCertSpecs.getKeySize();
            int caLifetimeDays = rootCertSpecs.getCertLifeTimeDays();
            String caSubject = rootCertSpecs.getSubject();
            String caSignatureAlgo = rootCertSpecs.getSignatureAlgo();

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
        } catch (CertSpecsValidationException
                | CertIOException
                | NoSuchAlgorithmException
                | OperatorCreationException
                | CertificateException ex) {
            logger.error("Error genarating root certitficate: " + ex.getMessage());
        }
    }

    public X509Certificate getServerCert() throws PkiException {
        return getServerCertModel().getCertificate();
    }

    public X509Certificate getUserCert(String username) throws PkiException {
        return getUserCertModel(username).getCertificate();
    }

    public PrivateKey getServerKey() throws PkiException {
        CertificateModel cm = getServerCertModel();
        return cm.getKeyModel().getPrivateKey();
    }

    public PrivateKey getUserKey(String username) throws PkiException {
        CertificateModel cm = getUserCertModel(username);
        return cm.getKeyModel().getPrivateKey();
    }

    public String getServerKeyAsBase64() throws PkiException {
        return asBase64(getServerKey());
    }

    public String getUserKeyAsBase64(String username) throws PkiException {
        return asBase64(getUserKey(username));
    }

    private CertificateModel getUserCertModel(String username)
            throws CertSpecsValidationException, PkiNotInitializedException {
        CertSpecs userCertSpecs = new CertSpecs(settings, CertSpecs.CertSpecType.USER_SPEC);
        userCertSpecs.validate();
        String subject = userCertSpecs
                .getSubject()
                .replace("{username}", username);
        List<CertificateModel> certModels
                = certificateRepository.findBySubjectIgnoreCaseAndCertType(
                        subject,
                        CertificateModel.CertType.USER);
        Date now = new Date();
        for (CertificateModel cm : certModels) {
            if (cm.getRevocationDate() == null && now.compareTo(cm.getValidTo()) < 0) {
                logger.info("Found user certificate");
                return cm;
            }
        }

        String keyAlgo = userCertSpecs.getKeyAlgo();
        int keySize = userCertSpecs.getKeySize();
        int lifetimeDays = userCertSpecs.getCertLifeTimeDays();
        String signatureAlgo = userCertSpecs.getSignatureAlgo();
        logger.info("Creating user certificate: " + subject);
        CertificateModel certModel = createCertificate(
                CertificateModel.CertType.USER,
                keyAlgo,
                keySize,
                lifetimeDays,
                subject,
                signatureAlgo);
        certificateRepository.save(certModel);

        return certModel;
    }

    private CertificateModel getServerCertModel()
            throws PkiNotInitializedException, CertSpecsValidationException {
        CertSpecs serverCertSpecs = new CertSpecs(settings, CertSpecs.CertSpecType.SERVER_SPEC);
        serverCertSpecs.validate();
        String subject = serverCertSpecs.getSubject();
        List<CertificateModel> certModels
                = certificateRepository.findBySubjectIgnoreCaseAndCertType(
                        subject,
                        CertificateModel.CertType.SERVER);
        Date now = new Date();
        for (CertificateModel cm : certModels) {
            if (cm.getRevocationDate() == null && now.compareTo(cm.getValidTo()) < 0) {
                return cm;
            }
        }

        String keyAlgo = serverCertSpecs.getKeyAlgo();
        int keySize = serverCertSpecs.getKeySize();
        int lifetimeDays = serverCertSpecs.getCertLifeTimeDays();
        String signatureAlgo = serverCertSpecs.getSignatureAlgo();
        logger.info("Creating server certificate: " + subject);
        CertificateModel certModel = createCertificate(
                CertificateModel.CertType.SERVER,
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
        } catch (PkiException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    public String getUserCertAsBase64(String username) {
        try {
            X509Certificate cert = getUserCert(username);
            return asBase64(cert);
        } catch (PkiException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    public X509Certificate createUserCert(String username) {
        return null;
    }

    private CertificateModel createCertificate(
            CertificateModel.CertType certType,
            String keyAlgo,
            int keySize,
            int lifeTimeDays,
            String subjectStr,
            String signingAlgo)
            throws PkiNotInitializedException {
        KeyPair keyPair;
        X509Certificate cert;
        try {
            CertSpecs rootCertSpecs = new CertSpecs(settings, CertSpecs.CertSpecType.CA_SPEC);
            X500Name rootSubject = new X500Name(rootCertSpecs.getSubject());
            X500Name subject = new X500Name(subjectStr);
            long serialLong = new SecureRandom().nextLong();
            BigInteger serial = new BigInteger(Long.toString(serialLong));
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
            JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(signingAlgo);

            ContentSigner csrContentSigner = csrBuilder.build(getRootKey());
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
            switch (certType) {
                case SERVER -> {
                    certBuilder.addExtension(
                            Extension.keyUsage,
                            false,
                            new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
                    );
                    certBuilder.addExtension(
                            Extension.extendedKeyUsage,
                            false,
                            new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
                    );
                }
                case USER -> {
                    certBuilder.addExtension(
                            Extension.keyUsage,
                            false,
                            new KeyUsage(KeyUsage.digitalSignature)
                    );
                    certBuilder.addExtension(
                            Extension.extendedKeyUsage,
                            false,
                            new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
                    );
                }
            }

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
                certType,
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
            PkiSettings pkiSettings = new PkiSettings(settings);
            pkiSettings.setDhParams(sw.toString());
            pkiSettings.save(settings);
        } catch (IOException ex) {
            logger.error("Error generating dh params: " + ex.getMessage());
        }
    }

    public String getDhParams() throws PkiNotInitializedException {
        PkiSettings pkiSettings = new PkiSettings(settings);
        return pkiSettings.getDhParams();
    }

    public X509CRL getCrl(Supplier<List<CertificateModel>> getCerts) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 7);
        try {
            // https://doc.primekey.com/bouncycastle/how-to-guides-pki-at-the-edge/how-to-generate-certificates-and-crls
            X509v2CRLBuilder crlGen = new JcaX509v2CRLBuilder(
                    getRootCert().getSubjectX500Principal(),
                    new Date()
            );
            crlGen.setNextUpdate(cal.getTime());
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            crlGen.addExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    extUtils.createAuthorityKeyIdentifier(getRootCert())
            );
            for (var cm : getCerts.get()) {
                CRLReason crlReason = CRLReason.lookup(CRLReason.privilegeWithdrawn);
                ExtensionsGenerator extGen = new ExtensionsGenerator();
                extGen.addExtension(Extension.reasonCode, false, crlReason);
                crlGen.addCRLEntry(
                        cm.getSerial(),
                        cm.getRevocationDate(),
                        extGen.generate());
            }
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC").build(getRootKey());
            JcaX509CRLConverter converter = new JcaX509CRLConverter()
                    .setProvider("BC");

            return converter.getCRL(crlGen.build(signer));
        } catch (CRLException
                | IOException
                | OperatorCreationException
                | CertificateEncodingException
                | NoSuchAlgorithmException ex) {
            logger.error("Cannot create CRL: " + ex.getMessage());
        }

        return crl;
    }
}
