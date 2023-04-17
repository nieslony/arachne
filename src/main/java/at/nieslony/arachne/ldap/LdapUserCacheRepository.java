/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface LdapUserCacheRepository extends JpaRepository<LdapUserCacheModel, Long> {

    Optional<LdapUserCacheModel> findByUsername(String username);
}
