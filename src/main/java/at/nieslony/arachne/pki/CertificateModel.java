/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

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
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
        CA,
        SERVER,
        USER
    }

    public CertificateModel(X509Certificate cert, CertType certType, KeyModel keyModel) {
        this.certificate = cert;
        this.subject = cert.getSubjectX500Principal().getName();
        this.certType = certType;
        this.validFrom = cert.getNotBefore();
        this.validTo = cert.getNotAfter();
        this.isRevoked = false;
        this.keyModel = keyModel;
    }

    public CertificateModel() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    @Lob
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
    private Boolean isRevoked;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "key_id")
    private KeyModel keyModel;
}
