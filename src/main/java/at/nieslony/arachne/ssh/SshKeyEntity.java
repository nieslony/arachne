/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.io.ByteArrayOutputStream;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Entity
@Getter
@Setter
public class SshKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    private String comment;
    private SshKeyType keyType;
    private int keySize;
    @Lob
    private String privateKey;
    @Lob
    private String publicKey;

    public SshKeyEntity() {
    }

    public static SshKeyEntity create(SshKeyType keyType, int keySize, String comment)
            throws JSchException {
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(
                jsch,
                keyType.getTypeNr(),
                keySize
        );

        ByteArrayOutputStream privbKeyStream = new ByteArrayOutputStream();
        keyPair.writePrivateKey(privbKeyStream);

        ByteArrayOutputStream pubKeyStream = new ByteArrayOutputStream();
        keyPair.writePublicKey(pubKeyStream, comment);

        SshKeyEntity entry = new SshKeyEntity();
        entry.keyType = keyType;
        entry.keySize = keySize;
        entry.comment = comment;
        entry.privateKey = privbKeyStream.toString();
        entry.publicKey = pubKeyStream.toString();

        return entry;
    }

    public String getLabel() {
        return "%s (%s %d)".formatted(comment, keyType.toString(), keySize);
    }
}
