/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package at.nieslony.arachne.ssh;

import com.jcraft.jsch.KeyPair;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author claas
 */
public enum SshKeyType {
    RSA(KeyPair.RSA, new Integer[]{2048, 4096}),
    ECDSA(KeyPair.ECDSA, new Integer[]{256, 512});

    private final int typeNr;
    private final List<Integer> keySizes;

    private SshKeyType(int typeNr, Integer[] keySizes) {
        this.typeNr = typeNr;
        this.keySizes = Arrays.asList(keySizes);
    }

    public int getTypeNr() {
        return typeNr;
    }

    public List<Integer> getKeySizes() {
        return keySizes;
    }

}
