/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 *
 * @author claas
 */
@Controller
public class VpnSiteController {

    private static final Logger logger = LoggerFactory.getLogger(VpnSiteController.class);

    public class VpnSiteNotFound extends Exception {

        public VpnSiteNotFound(Long id) {
            super("VpnSite with id %i not found".formatted(id));
        }
    }

    public class OnlyOneDefaultSiteAllowed extends Exception {

        public OnlyOneDefaultSiteAllowed() {
            super("Only one default site allowed");
        }
    }

    @Autowired
    VpnSiteRepository vpnSiteRepository;

    @PostConstruct
    public void init() {
        Optional<VpnSite> defaultSite = vpnSiteRepository.findByDefaultSite(true);
        if (defaultSite.isEmpty()) {
            createDefaultSite();
        }
    }

    private VpnSite createDefaultSite() {
        VpnSite defaultSite = VpnSite.builder()
                .defaultSite(true)
                .name("Default")
                .description("Default Settings for all Sites")
                .build();
        return vpnSiteRepository.save(defaultSite);
    }

    public List<VpnSite> getAll() {
        List<VpnSite> sites = vpnSiteRepository.findAll();
        return sites;
    }

    public List<VpnSite> getNonDefaultSites() {
        return vpnSiteRepository.findAllByDefaultSite(false);
    }

    public Optional<VpnSite> getById(long id) {
        return vpnSiteRepository.findById(id);
    }

    public VpnSite getDefaultSite() {
        return vpnSiteRepository.findByDefaultSite(true).get();
    }

    public VpnSite getDefaultSite(List<VpnSite> sites) {
        var retSite = sites.stream()
                .filter((site) -> site.isDefaultSite())
                .findFirst()
                .get();
        return retSite;
    }

    public VpnSite getSite(VpnSite site, List<VpnSite> sites) {
        var retSite = sites.stream()
                .filter((s) -> s.getId().equals(site.getId()))
                .findFirst()
                .orElse(getDefaultSite(sites));
        return retSite;
    }

    public VpnSite addSite(String name, String description) {
        VpnSite site = VpnSite.builder()
                .name(name)
                .description(description)
                .defaultSite(false)
                .build();
        logger.info("Adding site: " + site.toString());
        return vpnSiteRepository.save(site);
    }

    public VpnSite addSite(VpnSite site) throws OnlyOneDefaultSiteAllowed {
        logger.info("Adding site: " + site.toString());
        if (site.isDefaultSite()) {
            Optional<VpnSite> defSite = vpnSiteRepository.findByDefaultSite(true);
            if (defSite.isPresent()) {
                throw new OnlyOneDefaultSiteAllowed();
            }
        }
        return vpnSiteRepository.save(site);
    }

    public void deleteSite(VpnSite site) {
        vpnSiteRepository.deleteById(site.getId());
    }

    public VpnSite saveSite(VpnSite site) throws OnlyOneDefaultSiteAllowed {
        logger.info("Saving site: " + site.toString());
        if (site.isDefaultSite()) {
            Optional<VpnSite> defSite = vpnSiteRepository.findByDefaultSite(true);
            if (defSite.isPresent()) {
                if (site.getId() != null && !defSite.get().getId().equals(site.getId())) {
                    throw new OnlyOneDefaultSiteAllowed();
                }
                if (site.getId() == null) {
                    throw new OnlyOneDefaultSiteAllowed();
                }
            }
        }
        return vpnSiteRepository.save(site);
    }
}
