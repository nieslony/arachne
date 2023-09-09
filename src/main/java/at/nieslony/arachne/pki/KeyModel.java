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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PrivateKey;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(KeyModel.class);

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

    public byte[] getEncoded() {
        if (privateKey != null) {
            try (
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(privateKey);

                return bos.toByteArray();
            } catch (IOException ex) {
                logger.error("Cannot serialize private key: " + ex.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public void setEncoded(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bis)) {
            privateKey = (PrivateKey) ois.readObject();
        } catch (ClassNotFoundException | IOException ex) {
            logger.error("Cannot deserialize private key: " + ex.getMessage());
        }
    }
}
