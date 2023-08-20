/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.utils.pki;

import java.io.InputStream;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author claas
 */
public class CertificateAuthorityTest {

    public CertificateAuthorityTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setCaDir method, of class CertificateAuthority.
     */
    @Test
    public void testSetGetCaDir() {
        System.out.println("setCaDir/getCaDir");
        String caDir = "somename";
        CertificateAuthority instance = new CertificateAuthority();
        instance.setCaDir(caDir);
        String s = instance.getCaDir();
        assertEquals(s, caDir);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of createSelfSignedCa method, of class CertificateAuthority.
     */
    @Test
    public void testCreateSelfSignedCa() throws Exception {
        final String[] signatureAlgorithms = {
            "SHA1withDSA",
            /*"SHA1withECDSA",
            "SHA224withECDSA",
            "SHA256withECDSA",
            "SHA384withECDSA",
            "SHA512withECDSA",
            "GOST3411withGOST3410",
            "GOST3411withECGOST3410",
            "MD2withRSA",
            "MD5withRSA",
            "SHA1withRSA",
            "SHA224withRSA",
            "SHA256withRSA",
            "SHA384withRSA",
            "SHA512withRSA",
            "RIPEMD128withRSA",
            "RIPEMD160withRSA",
            "RIPEMD256withRSA"*/
        };

        System.out.println("createSelfSignedCa");
        Time startDate = new Time(new Date());
        Time endDate = new Time(new Date(startDate.getTime() + 1000L * 60L * 60L * 24L * 3650L));
        X500Name issuerDN = new X500Name("cn=Test Issuer");
        X500Name subjectDN = new X500Name("cn=Test CA");
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            CertificateAuthority instance = new CertificateAuthority();
            for (String sa : ksa.signatureAlgos) {
                String signingAlgo = sa + "with" + ksa.keyAlgo;
                System.out.println("  " + signingAlgo);
                System.out.print("  ");
                int keySizes[] = ksa.keySizes;
                for (int keySize : keySizes) {
                    System.out.print(" " + keySize);
                    try {
                        instance.createSelfSignedCa(startDate, endDate, issuerDN, subjectDN, signingAlgo, ksa.keyAlgo, keySize);
                    }
                    catch (GeneralSecurityException ex) {
                        fail("Failed: " + signingAlgo + " " + keySize + "bit");
                    }
                }
                System.out.println("");
            }
        }
        /*for (String sa : signatureAlgorithms) {
            instance.createSelfSignedCa(startDate, endDate, issuerDN, subjectDN, sa, keyAlgorithm, keySize);
        }*/
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of createCertificate method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testCreateCertificate() throws Exception {
        System.out.println("createCertificate");
        PublicKey publicKey = null;
        Time startDate = null;
        Time endData = null;
        X500Name subjectDN = null;
        CertificateAuthority instance = new CertificateAuthority();
        X509CertificateHolder expResult = null;
        X509CertificateHolder result = instance.createCertificate(publicKey, startDate, endData, subjectDN,
                "SHA256withRSA");
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writeCertificate method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testWriteCertificate() throws Exception {
        System.out.println("writeCertificate");
        X509CertificateHolder cert = null;
        PrintWriter out = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.writeCertificate(cert, out);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writeCaCert method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testWriteCaCert() throws Exception {
        System.out.println("writeCaCert");
        PrintWriter out = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.writeCaCert(out);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of readPrivateKey method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testReadPrivateKey() throws Exception {
        System.out.println("readPrivateKey");
        InputStream in = null;
        CertificateAuthority instance = new CertificateAuthority();
        PrivateKey expResult = null;
        PrivateKey result = instance.readPrivateKey(in);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of readCertificate method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testReadCertificate() throws Exception {
        System.out.println("readCertificate");
        InputStream in = null;
        CertificateAuthority instance = new CertificateAuthority();
        X509Certificate expResult = null;
        X509Certificate result = instance.readCertificate(in);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writePrivateKey method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testWritePrivateKey() throws Exception {
        System.out.println("writePrivateKey");
        PrivateKey key = null;
        PrintWriter out = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.writePrivateKey(key, out);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writeDhParameters method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testWriteDhParameters() throws Exception {
        System.out.println("writeDhParameters");
        PrintWriter out = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.writeDhParameters(out);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writeCaKey method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testWriteCaKey() throws Exception {
        System.out.println("writeCaKey");
        PrintWriter out = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.writeCaKey(out);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCaCert method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testSetCaCert() {
        System.out.println("setCaCert");
        X509CertificateHolder cert = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.setCaCert(cert);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCaCert method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testGetCaCert() {
        System.out.println("getCaCert");
        CertificateAuthority instance = new CertificateAuthority();
        X509CertificateHolder expResult = null;
        X509CertificateHolder result = instance.getCaCert();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCaKey method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testSetCaKey() {
        System.out.println("setCaKey");
        PrivateKey key = null;
        CertificateAuthority instance = new CertificateAuthority();
        instance.setCaKey(key);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCaKey method, of class CertificateAuthority.
     */
    @Ignore
    @Test
    public void testGetCaKey() {
        System.out.println("getCaKey");
        CertificateAuthority instance = new CertificateAuthority();
        PrivateKey expResult = null;
        PrivateKey result = instance.getCaKey();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
