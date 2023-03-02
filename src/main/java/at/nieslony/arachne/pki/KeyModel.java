/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import java.io.Serializable;
import java.security.PrivateKey;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.Data;

/**
 *
 * @author claas
 */
@Data
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
    private PrivateKey privateKey;
}
