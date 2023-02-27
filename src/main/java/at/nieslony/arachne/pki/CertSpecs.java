/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import lombok.Data;

/**
 *
 * @author claas
 */
@Data
public class CertSpecs {

    private String keyAlgo;
    private int keySize;
    private int certLifeTimeDays;
    private String subject;
    private String signatureAlgo;
}
