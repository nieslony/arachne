/*
 * Copyright (C) 2024 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.apiindex.ApiDescription;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/openvpn")
public class OpenVpnRestController {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnRestController.class);

    @Autowired
    OpenVpnController openVpnController;

    @Autowired
    Settings settings;

    @Autowired
    private VpnSiteRepository vpnSiteRepository;

    @GetMapping("auth")
    @RolesAllowed(value = {"USER"})
    public String auth() {
        return "Authenticated";
    }

    @GetMapping("/user_settings")
    @RolesAllowed(value = {"ADMIN"})
    public OpenVpnUserSettings getUserSettings(OpenVpnController openVpnController) {
        return settings.getSettings(OpenVpnUserSettings.class);
    }

    @PostMapping("/user_settings")
    @RolesAllowed(value = {"ADMIN"})
    public OpenVpnUserSettings postUserSettings(
            @RequestBody OpenVpnUserSettings vpnSettings
    ) throws SettingsException {
        logger.info("Set new openVPN user server config: " + settings.toString());
        vpnSettings.save(settings);
        openVpnController.writeOpenVpnUserServerConfig(vpnSettings);
        return vpnSettings;
    }

    @GetMapping("/user_config/{username}")
    @RolesAllowed(value = {"ADMIN"})
    public String getUserVpnConfig(
            @PathVariable String username,
            @RequestParam(required = false, name = "format") String format
    ) throws SettingsException {
        try {
            if (format == null) {
                return openVpnController.openVpnUserConfig(username);
            }
            logger.info("Return format: " + format);
            return switch (format) {
                case "json" ->
                    openVpnController.openVpnUserConfigJson(username);
                case "shell" ->
                    openVpnController.openVpnUserConfigShell(username);
                default ->
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_CONTENT,
                            "Cannot get user config");
            };
        } catch (PkiException | JSONException ex) {
            logger.error("Cannot create user config: " + ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Cannot get user config");
        }
    }

    @GetMapping("/user_config")
    @RolesAllowed(value = {"USER"})
    public String getUserVpnConfig(
            @RequestParam(required = false, name = "format")
            @ApiDescription(
                    isHtml = true,
                    value = """
                    <strong>shell</strong>: shell script to add connection to NetworkManager,<br>
                    <strong>json</strong>: internally used by ArchneConfigDownloader
                    """
            ) String format
    ) throws SettingsException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return getUserVpnConfig(username, format);
    }

    @GetMapping("/site")
    @RolesAllowed(value = {"ADMIN"})
    public List<VpnSite> getSiteVpnSite() {
        return vpnSiteRepository.findAll();
    }

    @GetMapping("/site/{id}")
    @RolesAllowed(value = {"ADMIN"})
    public VpnSite getSiteVpnSite(@PathVariable Long id) {
        return vpnSiteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "VPN Site %d not found".formatted(id)));
    }

}
