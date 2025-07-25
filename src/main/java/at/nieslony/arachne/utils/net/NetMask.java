/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.net;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@EqualsAndHashCode
public class NetMask {

    @Getter
    @Setter
    private int bits;

    public NetMask() {
    }

    public NetMask(int bits) {
        this.bits = bits;
    }

    public String getMask() {
        return NetUtils.maskLen2Mask(bits);
    }

    @Override
    public String toString() {
        return format(bits);
    }

    public static String format(int bits) {
        String mask = NetUtils.maskLen2Mask(bits);
        return "%d - %s".formatted(bits, mask);
    }
}
