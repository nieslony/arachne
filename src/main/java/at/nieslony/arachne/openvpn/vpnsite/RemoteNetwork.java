/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.vpnsite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author claas
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RemoteNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String address = "1.2.3.0";
    private int mask = 24;
    private String name = "";

    @Override
    public String toString() {
        return ObjectUtils.isEmpty(name) ? "%s/%d".formatted(address, mask) : "%s/%d (%s)".formatted(address, mask, name);
    }

}
