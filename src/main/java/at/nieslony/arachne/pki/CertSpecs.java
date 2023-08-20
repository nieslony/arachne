/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import at.nieslony.arachne.settings.Settings;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class CertSpecs {

    public enum CertSpecKey {
        SK_KEY_ALGO("keyAlgo"),
        SK_KEY_SIZE("keySize"),
        SK_CERT_LIFETIME_DAYS("certLifeTimeDays"),
        SK_SUBJECT("subject"),
        SK_SIGNATURE_ALGO("signatureAlgo"),
        SK_CERT_SPEC_TYPE("certSpecType");

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

    private final static String SK_PREFIX = "cert-specs";

    private static String getSettingKey(CertSpecType certSpecType, CertSpecKey field) {
        return SK_PREFIX + "." + certSpecType + "." + field;
    }

    private String keyAlgo;
    private int keySize;
    private int certLifeTimeDays;
    private String subject;
    private String signatureAlgo;
    private CertSpecType certSpecType;

    public CertSpecs() {
    }

    public CertSpecs(Settings settings, CertSpecType certSpecType) {
        this.certSpecType = certSpecType;
        this.keyAlgo = settings.get(
                getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_KEY_ALGO),
                ""
        );
        this.keySize = settings.getInt(
                getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_KEY_SIZE),
                2048
        );
        this.certLifeTimeDays = settings.getInt(
                getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_CERT_LIFETIME_DAYS),
                switch (certSpecType) {
            case CA_SPEC:
                yield 10 * 365;
            case SERVER_SPEC:
                yield 2 * 365;
            case USER_SPEC:
                yield 365;
        }
        );
        this.subject = settings.get(
                getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_SUBJECT),
                ""
        );
        this.signatureAlgo = settings.get(
                getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_SIGNATURE_ALGO),
                ""
        );
    }

    public void save(Settings settings, CertSpecType certSpecType) {
        this.certSpecType = certSpecType;
        save(settings);
    }

    public void save(Settings settings) {
        settings.put(getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_KEY_ALGO), keyAlgo);
        settings.put(getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_KEY_SIZE), keySize);
        settings.put(getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_CERT_LIFETIME_DAYS), certLifeTimeDays);
        settings.put(getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_SIGNATURE_ALGO), signatureAlgo);
        settings.put(getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_SUBJECT), subject);
        settings.put(getSettingKey(certSpecType, CertSpecs.CertSpecKey.SK_CERT_SPEC_TYPE), certSpecType);
    }

    public void validate() throws CertSpecsValidationException {
        if (getCertSpecType() == null) {
            throw new CertSpecsValidationException("Unknown certSpecType");
        }

        KeyPairGenerator testKeyPairGenerator;
        if (getKeyAlgo() == null) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_KEY_ALGO, "not provided");
        }
        try {
            testKeyPairGenerator = KeyPairGenerator.getInstance(getKeyAlgo());
        } catch (NoSuchAlgorithmException ex) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_KEY_ALGO, ex.getMessage());
        }
        KeyPair testKey = testKeyPairGenerator.generateKeyPair();

        if (getKeySize() == 0) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_KEY_SIZE, "must be > 0");
        }
        try {
            testKeyPairGenerator.initialize(getKeySize());
        } catch (InvalidParameterException ex) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_KEY_SIZE, ex.getMessage());
        }

        if (getCertLifeTimeDays() == 0) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_CERT_LIFETIME_DAYS, "must be > 0");
        }

        if (getSubject() == null) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_SUBJECT, "not provided");
        }
        try {
            X500Name sbj;
            sbj = new X500Name(getSubject());
        } catch (IllegalArgumentException ex) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_SUBJECT, ex.getMessage());
        }

        if (getSignatureAlgo() == null) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_SIGNATURE_ALGO, "not provided");
        }
        try {
            new JcaContentSignerBuilder(getSignatureAlgo())
                    .build(testKey.getPrivate());
        } catch (OperatorCreationException | IllegalArgumentException ex) {
            throw new CertSpecsValidationException(certSpecType, CertSpecKey.SK_SIGNATURE_ALGO, ex.getMessage());
        }
    }
}
