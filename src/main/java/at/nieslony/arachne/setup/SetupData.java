/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.setup;

import at.nieslony.arachne.pki.CertSpecs;
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
public class SetupData {

    private String adminUsername = "admin";
    @ToString.Exclude
    private String adminPassword;
    private String adminEmail;

    private CertSpecs caCertSpecs;
    private CertSpecs serverCertSpecs;
    private CertSpecs userCertSpecs;
}
