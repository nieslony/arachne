/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface KeyRepository extends JpaRepository<KeyModel, Long> {
}
