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
@Getter
@Setter
@EqualsAndHashCode
public class NicInfo {

    String ipAddress;
    private String nicName;

    public NicInfo(String ipAddress, String nicName) {
        this.ipAddress = ipAddress;
        this.nicName = nicName;
    }

    public NicInfo() {
    }

    @Override
    public String toString() {
        return "%s - %s".formatted(ipAddress, nicName);
    }

}
