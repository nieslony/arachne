/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;

/**
 *
 * @author claas
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "certitificates")
public class CertificateModel implements Serializable {

    public enum CertType {
        INVALID("Invalid"),
        CA("Certificate Authority"),
        SERVER("Server"),
        USER("User");

        private final String description;

        private CertType(String d) {
            description = d;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public CertificateModel(X509Certificate cert, CertType certType, KeyModel keyModel) {
        this.certificate = cert;
        this.subject = cert.getSubjectX500Principal().getName();
        this.certType = certType;
        this.validFrom = cert.getNotBefore();
        this.validTo = cert.getNotAfter();
        this.revocationDate = null;
        this.keyModel = keyModel;
    }

    public CertificateModel() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    @Lob
    @JsonIgnore
    private X509Certificate certificate;

    @Column
    private String subject;

    @Column
    private CertType certType;

    @Column
    private Date validFrom;

    @Column
    private Date validTo;

    @Column
    private Date revocationDate;

    @Column
    private BigInteger serial;

    public BigInteger getSerial() {
        if (serial != null) {
            return serial;
        } else {
            return certificate.getSerialNumber();
        }
    }

    public byte[] getEncoded() {
        if (certificate != null) {
            try {
                return certificate.getEncoded();
            } catch (CertificateEncodingException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    public void setEncoded(byte[] bytes) {
        CertificateFactory certFactory = new CertificateFactory();
        try {
            certificate = (X509Certificate) certFactory.engineGenerateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException ex) {

        }
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "key_id")
    private KeyModel keyModel;
}
