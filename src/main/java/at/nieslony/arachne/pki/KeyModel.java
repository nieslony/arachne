/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "keys")
public class KeyModel implements Serializable {

    public KeyModel(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public KeyModel() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(nullable = false)
    @Lob
    @JsonIgnore
    private PrivateKey privateKey;

    @JsonIgnore
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private byte[] bytes;

    @JsonIgnore
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private String keyAlgo;

    public byte[] getEncoded() {
        if (privateKey != null) {
            return privateKey.getEncoded();
        } else {
            return null;
        }
    }

    private void loadKey() {
        try {
            KeyFactory kf = KeyFactory.getInstance(keyAlgo);
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {

        }
    }

    public void setEncoded(byte[] bytes) {
        this.bytes = bytes;
        if (keyAlgo != null) {
            loadKey();
        }
    }

    public String getAlgorithm() {
        return privateKey.getAlgorithm();
    }

    public void setAlgorithm(String algo) {
        keyAlgo = algo;
        if (bytes != null) {
            loadKey();
        }
    }
}
