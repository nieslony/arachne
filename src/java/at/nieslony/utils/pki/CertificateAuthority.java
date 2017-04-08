/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.utils.pki;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import javax.xml.bind.DatatypeConverter;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

/**
 *
 * @author claas
 */
public class CertificateAuthority {
    private String caDir;
    private X509Certificate caCert;
    private PrivateKey caKey;
    private X509CRL crl;

    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
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

    public void createSelfSignedCa(Date startDate, Date endDate,
            X500Principal issuerDN, X500Principal subjectDN,
            String signatureAlgorithm,
            String keyAlgorithm, int keySize
    ) throws GeneralSecurityException {
        BigInteger serialNumber = new BigInteger(keySize, new Random());

        KeyPairGenerator keygen = KeyPairGenerator.getInstance(keyAlgorithm);
        keygen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keygen.genKeyPair();
        caKey = keyPair.getPrivate();

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(serialNumber);
        certGen.setSignatureAlgorithm(signatureAlgorithm);
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(endDate);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setIssuerDN(issuerDN);
        certGen.setSubjectDN(subjectDN);

        //try {
            caCert = certGen.generate(caKey);
        /*}
        catch (GeneralSecurityException ex) {
            logger.severe(String.format("Cannot create self signed certificate: %s",
                    ex.getMessage()));
        }*/
    }

    public X509Certificate createCertificate(
            PublicKey publicKey,
            Date startDate, Date endData,
            X500Principal subjectDN,
            String signatureAlgorithm
) throws GeneralSecurityException {
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(subjectDN);
        certGen.setIssuerDN(caCert.getSubjectX500Principal());
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(endData);
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm(signatureAlgorithm);

        X509Certificate cert = certGen.generate(caKey);
        return cert;
    }

    public void writeCertificate(X509Certificate cert, PrintWriter out)
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

    private void writeBase64(byte[] data, PrintWriter out) throws IOException {
        final int MAX_LINE = 64;

        String base64 = DatatypeConverter.printBase64Binary(data);
        for (int i = 0; i < base64.length(); i += MAX_LINE) {
            int end = Math.min(i + MAX_LINE, base64.length());
            out.println(base64.substring(i, end));
            //out.write("\n");
        }
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

    public X509CRL readCrl(InputStream in)
            throws IOException, CRLException, CertificateException
    {
        byte[] data = readPEM(in, "X509 CRL");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");


        X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(data));
        Set<? extends X509CRLEntry> certs = crl.getRevokedCertificates();
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
        seq.add(new DERInteger(realParams.getP()));
        seq.add(new DERInteger(realParams.getG()));
        byte[] encoded = new DERSequence(seq).getEncoded();
        out.println("-----BEGIN DH PARAMETERS-----");
        writeBase64(encoded, out);
        out.println("-----END DH PARAMETERS-----");
        logger.info("DH parameters written");
    }

    public void writeCaKey(PrintWriter out) throws IOException {
        writePrivateKey(caKey, out);
    }

    public void setCaCert(X509Certificate cert) {
        this.caCert = cert;
    }

    public X509Certificate getCaCert() {
        return caCert;
    }

    public void setCaKey(PrivateKey key) {
        this.caKey = key;
    }

    public PrivateKey getCaKey() {
        return caKey;
    }

    public void createCrl()
            throws CertificateParsingException, CRLException,
            NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException
    {
        X509V2CRLGenerator crlGen = new X509V2CRLGenerator();
        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + 1000 * 60 * 60 * 24 * 31);

        crlGen.setIssuerDN(caCert.getSubjectX500Principal());
        crlGen.setThisUpdate(now);
        crlGen.setNextUpdate(nextUpdate);
        crlGen.setSignatureAlgorithm(caCert.getSigAlgName());
        crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        crlGen.addExtension(X509Extensions.CRLNumber, false, new CRLNumber(BigInteger.ZERO));
        crl = crlGen.generate(caKey);
    }

    public void writeCrl(PrintWriter pw)
            throws CRLException, IOException
    {
        pw.println("-----BEGIN X509 CRL-----");
        writeBase64(crl.getEncoded(), pw);
        pw.println("-----END X509 CRL-----");
        pw.flush();
    }

    public void setCrl(X509CRL crl) {
        this.crl = crl;
    }

    public void addCertificateToCrl(X509Certificate cert)
        throws CertificateParsingException, CRLException,
            NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException
    {
        X509V2CRLGenerator crlGen = new X509V2CRLGenerator();
        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + 1000 * 60 * 60 * 24 * 31);

        crlGen.setIssuerDN(caCert.getSubjectX500Principal());
        crlGen.setThisUpdate(now);
        crlGen.setNextUpdate(nextUpdate);
        crlGen.setSignatureAlgorithm(caCert.getSigAlgName());
        crlGen.addCRL(crl);
        crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        crlGen.addExtension(X509Extensions.CRLNumber, false, new CRLNumber(BigInteger.ZERO));
        crlGen.addCRLEntry(cert.getSerialNumber(), now, CRLReason.privilegeWithdrawn);
        crl = crlGen.generate(caKey);
    }

    public boolean isCertificateRevoked(X509Certificate cert) {
        return crl.isRevoked(cert);
    }
}
