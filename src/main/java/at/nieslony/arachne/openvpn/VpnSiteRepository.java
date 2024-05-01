/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface VpnSiteRepository extends JpaRepository<VpnSite, Long> {

    Optional<VpnSite> findByDefaultSite(boolean isDefaultSite);

    List<VpnSite> findAllByDefaultSite(boolean isDefaultSite);
}
