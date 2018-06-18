/*
 * Copyright (C) 2018 Claas Nieslony
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

package at.nieslony.utils.pki;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.AlgorithmNameFinder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 *
 * @author claas
 */
public class CertificateAuthority
        implements Serializable
{
    private String caDir;
    transient private X509CertificateHolder caCert;
    transient private PrivateKey caKey;
    transient private X509CRLHolder crl;

    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public void init() {
        try {
            Provider provs[] = Security.getProviders();
            boolean found = false;
            for (Provider p: provs) {
                if (p.getName().equals(BouncyCastleProvider.PROVIDER_NAME)) {
                    logger.info("BouncyCastleProvider already added");
                    found = true;
                    break;
                }
            }
            if (!found) {
                logger.info("Adding new security privider: BouncyCastleProvider");
                Security.addProvider(new BouncyCastleProvider());
            }
        }
        catch (SecurityException ex) {
            logger.severe(String.format("Cannot add security provider: %s",
                    ex.getMessage()));
        }
    }

    public void destroy() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    public static class KeySignAlgo {
        public String keyAlgo;
        public String[] signatureAlgos;
        public int[] keySizes;

        private KeySignAlgo(String ka, int[] ks, String[] sa) {
            keyAlgo = ka;
            signatureAlgos = sa;
            keySizes = ks;
        }
    }
    static final KeySignAlgo[] keySignAlgos = {
        new KeySignAlgo("DSA",
                new int[]{ 512, 1024 },
                new String[]{
                    "SHA1"
                }),
        new KeySignAlgo("RSA",
                new int[]{ 1024, 2048, 4096 },
                new String[]{
                    "MD2",
                    "MD5",
                    "SHA1",
                    "SHA224",
                    "SHA256",
                    "SHA384",
                    "SHA512",
                    "RIPEMD128",
                    "RIPEMD160",
                    "RIPEMD256"
                }),
    };

    static public KeySignAlgo[] getKeySignAlgos() {
        return keySignAlgos;
    }

    public void setCaDir(String caDir) {
        this.caDir = caDir;
    }

    public String getCaDir() {
        return caDir;
    }

    public void createSelfSignedCa(Time startDate, Time endDate,
            X500Name issuerDN, X500Name subjectDN,
            String signatureAlgorithm,
            String keyAlgorithm, int keySize
    ) throws GeneralSecurityException {
        BigInteger serialNumber = new BigInteger(keySize, new Random());

        KeyPairGenerator keygen = KeyPairGenerator.getInstance(keyAlgorithm);
        keygen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keygen.genKeyPair();

        SubjectPublicKeyInfo subPubKeyInfo =
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        caKey = keyPair.getPrivate();

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
            issuerDN,
                serialNumber,
                startDate, endDate,
                subjectDN,
                subPubKeyInfo);
        try {
            certBuilder.addExtension(Extension.basicConstraints, true,
                    new BasicConstraints(0));
        }
        catch (CertIOException ex) {
            logger.severe(String.format("Cannot add certificate extension: %s",
                    ex.getMessage()));
            return;
        }

        logger.info(String.format("Creating new content signer: %s", signatureAlgorithm));
        ContentSigner sigGen;
        try {
             sigGen = new JcaContentSignerBuilder(
                     //"SHA256withRSA"
                     signatureAlgorithm
                             )
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keyPair.getPrivate());
        }
        catch (OperatorCreationException | IllegalStateException ex) {
            logger.severe(String.format("Cannot create certificate signer: %s",
                    ex.getMessage()));
            return;
        }

        caCert = certBuilder.build(sigGen);
    }

    public X509CertificateHolder createCertificate(
            PublicKey publicKey,
            Time startDate, Time endDate,
            X500Name subjectDN,
            String signatureAlgorithm
    ) throws OperatorCreationException {
        if (caCert == null || publicKey == null) {
            logger.warning("caCert and caKey must not be null");
            return null;
        }

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                caCert.getSubject(),
                BigInteger.valueOf(System.currentTimeMillis()),
                startDate, endDate,
                subjectDN,
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));

        ContentSigner sigGen = new JcaContentSignerBuilder(signatureAlgorithm)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(caKey);

        return certBuilder.build(sigGen);
    }

    public void writeCertificate(X509CertificateHolder cert, PrintWriter out)
        throws IOException, CertificateEncodingException
    {
        out.println("-----BEGIN CERTIFICATE-----");
        if (cert != null)
            writeBase64(cert.getEncoded(), out);
        out.println("-----END CERTIFICATE-----");
        out.flush();
    }

    public void writeCaCert(PrintWriter out)
        throws IOException, CertificateEncodingException
    {
        writeCertificate(caCert, out);
    }

    public void writeCsr(PKCS10CertificationRequest csr, PrintWriter out)
            throws IOException
    {
        out.println("-----BEGIN CERTIFICATE REQUEST-----");
        writeBase64(csr.getEncoded(), out);
        out.println("-----END CERTIFICATE REQUEST-----");
    }

    private void writeBase64(byte[] data, PrintWriter out) throws IOException {
        final int MAX_LINE = 64;

        String base64 = DatatypeConverter.printBase64Binary(data);
        for (int i = 0; i < base64.length(); i += MAX_LINE) {
            int end = Math.min(i + MAX_LINE, base64.length());
            out.println(base64.substring(i, end));
            //out.write("\n");
        }
    }

    public void setCaCert(String certAsPem)
            throws IOException
    {
        InputStream is = new ByteArrayInputStream(certAsPem.getBytes());

        byte[] bytes = readPEM(is, "CERTIFICATE");

        X509CertificateHolder cert = new X509CertificateHolder(bytes);
        setCaCert(cert);
    }

    private byte[] readPEM(InputStream in, String expectedBeginEnd)
            throws IOException
    {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder base64 = new StringBuilder();
        String line = br.readLine();
        String matchBegin = "-----BEGIN " + expectedBeginEnd + "-----";
        String matchEnd = "-----END " + expectedBeginEnd + "-----";
        try {
            while (!line.matches(matchBegin)) {
                line = br.readLine();
            }
        }
        catch (NullPointerException ex) {
            logger.severe(String.format("Unexpected eof of file. No line matching %s",
                    matchBegin));
            return new byte[0];
        }
        line = br.readLine();
        try {
            while (!line.matches(matchEnd)) {
                base64.append(line.trim());
                line = br.readLine();
            }
        }
        catch (NullPointerException ex) {
            logger.severe(String.format("Unexpected eof of file. No line matching %s",
                    matchEnd));
            return new byte[0];
        }

        return DatatypeConverter.parseBase64Binary(base64.toString());
    }

    public PrivateKey readPrivateKey(InputStream in) throws IOException, GeneralSecurityException {
        byte[] data = readPEM(in, "PRIVATE KEY");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(data));

        return key;
    }

    public X509CRLHolder readCrl(InputStream in)
            throws IOException, CRLException, CertificateException
    {
        X509CRLHolder crl = new X509CRLHolder(in);

        Collection<? extends X509CRLEntry> certs = crl.getRevokedCertificates();
        if (certs != null) {
            logger.info(String.format("Found %d revoked certificates", crl.getRevokedCertificates().size()));
            for (X509CRLEntry entry : certs) {
                String serial = entry.getSerialNumber().toString(16);
                logger.info(String.format("   serial: %s", serial));
            }
        }
        else {
            logger.info("Found no revoked certificates");
        }

        return crl;
    }

    public X509Certificate readCertificate(InputStream in) throws IOException, CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        return cert;
    }

    public void writePrivateKey(PrivateKey key, PrintWriter out) throws IOException {
        out.println("-----BEGIN PRIVATE KEY-----");
        if (key != null)
            writeBase64(key.getEncoded(), out);
        out.println("-----END PRIVATE KEY-----");
        out.flush();
    }

    public void writeDhParameters(PrintWriter out) throws IOException {
        logger.info("Creating DH parameters. This can take a while...");
        DHParametersGenerator generator = new DHParametersGenerator();
        generator.init(1024, 80, new SecureRandom());
        DHParameters params = generator.generateParameters();
        DHParameters realParams = new DHParameters(params.getP(), BigInteger.valueOf(2));
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(new ASN1Integer(realParams.getP()));
        seq.add(new ASN1Integer(realParams.getG()));
        byte[] encoded = new DERSequence(seq).getEncoded();
        out.println("-----BEGIN DH PARAMETERS-----");
        writeBase64(encoded, out);
        out.println("-----END DH PARAMETERS-----");
        logger.info("DH parameters written");
    }

    public void writeCaKey(PrintWriter out) throws IOException {
        writePrivateKey(caKey, out);
    }

    public void setCaCert(X509CertificateHolder cert) {
        this.caCert = cert;
    }

    public X509CertificateHolder getCaCert() {
        return caCert;
    }

    public void setCaKey(PrivateKey key) {
        this.caKey = key;
    }

    public PrivateKey getCaKey() {
        return caKey;
    }

    public void createCrl()
            throws CRLException, OperatorCreationException, CertIOException,
            IOException
    {
        if (caCert == null) {
            logger.warning("Cannot create CRL: There's no ca certificate");
        }
        if (caKey == null) {
            logger.warning("Cannot create CRL: There's no ca private key");
        }

        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + (long) 1000 * 60 * 60 * 24 * 31);
        BcX509ExtensionUtils extUtils = new BcX509ExtensionUtils();

        X500Name crlName = new X500Name("cn=CRL");

        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(crlName, now);
        crlBuilder.setNextUpdate(nextUpdate);

        AlgorithmNameFinder algoFinder = new DefaultAlgorithmNameFinder();
        String signAlgoName = algoFinder.getAlgorithmName(caCert.getSignatureAlgorithm());

        ContentSigner sigGen = new JcaContentSignerBuilder(signAlgoName)
                    .setProvider("BC")
                    .build(caKey);
        crl = crlBuilder.build(sigGen);
    }

    public void writeCrl(PrintWriter pw)
            throws CRLException, IOException
    {
        pw.println("-----BEGIN X509 CRL-----");
        writeBase64(crl.getEncoded(), pw);
        pw.println("-----END X509 CRL-----");
        pw.flush();
    }

    public void setCrl(X509CRLHolder crl) {
        this.crl = crl;
    }

    public void addCertificateToCrl(X509CertificateHolder cert)
        throws CertIOException, OperatorCreationException
    {
        if (cert == null) {
            logger.warning("Cannot add null certificate to CRL");
            return;
        }

        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + (long) 1000 * 60 * 60 * 24 * 31);
        BcX509ExtensionUtils extUtils = new BcX509ExtensionUtils();
        X500Name crlName = new X500Name("cn=CRL");

        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(crlName, now);
        crlBuilder.setNextUpdate(nextUpdate);

        AuthorityKeyIdentifier aki = extUtils.createAuthorityKeyIdentifier(caCert);
        crlBuilder.addExtension(Extension.authorityKeyIdentifier, false, aki);

        crlBuilder.addCRL(crl);
        crlBuilder.addCRLEntry(cert.getSerialNumber(), now, CRLReason.superseded);

        AlgorithmNameFinder algoFinder = new DefaultAlgorithmNameFinder();
        String signAlgoName = algoFinder.getAlgorithmName(caCert.getSignatureAlgorithm());

        ContentSigner sigGen = new JcaContentSignerBuilder(signAlgoName)
                    .setProvider("BC")
                    .build(caKey);
        crl = crlBuilder.build(sigGen);
    }

    public boolean isCertificateRevoked(X509CertificateHolder cert) {
        if (cert == null) {
            return false;
        }

        if (cert.getSerialNumber() == null) {
            logger.warning("Certificate has no serial");
            return false;
        }

        if (crl == null)
            return false;
        return crl.getRevokedCertificate(cert.getSerialNumber()) != null;
    }

    public String getCertCn(X509CertificateHolder cert) {
        RDN cn = cert.getSubject().getRDNs(BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }

    public PKCS10CertificationRequest createCaCsr(X500Name subjectDN,
            String signatureAlgorithm,
            String keyAlgorithm, int keySize)
        throws NoSuchAlgorithmException, OperatorCreationException
    {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance(keyAlgorithm);
        keygen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keygen.genKeyPair();

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
            subjectDN, keyPair.getPublic());

        ContentSigner sigGen;
        sigGen = new JcaContentSignerBuilder(signatureAlgorithm)
               .setProvider(BouncyCastleProvider.PROVIDER_NAME)
               .build(keyPair.getPrivate());

        caKey = keyPair.getPrivate();
        caCert = null;

        return p10Builder.build(sigGen);
    }
}
