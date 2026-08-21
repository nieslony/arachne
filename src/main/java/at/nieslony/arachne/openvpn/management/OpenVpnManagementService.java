/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.openvpn.management;

import at.nieslony.arachne.utils.FolderFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 *
 * @author claas
 */
@Component
@ApplicationScope
@Slf4j
public class OpenVpnManagementService
        implements BeanFactoryAware, ApplicationListener<ApplicationContextInitializedEvent> {

    @Autowired
    private FolderFactory folderFactory;

    private BeanFactory beanFactory;

    private OpenVpnManagementIf userManagementIf;
    private OpenVpnManagementIf siteManagementIf;

    @PostConstruct
    public void init() {
        log.info("Initializing Management Interface");
        userManagementIf = new OpenVpnUserManagementIf(beanFactory);
        siteManagementIf = new OpenVpnSiteManagementIf(beanFactory);

        userManagementIf.run();
        siteManagementIf.run();
    }

    public void done() {
        log.info("PreDestroy");
        userManagementIf.stop();
        siteManagementIf.stop();
    }

    public String getSiteManagementSocket() {
        return "%s/openvpn-site-management.sock".formatted(
                folderFactory.getOpenVpnRunDir()
        );
    }

    public String getUserManagementSocket() {
        return "%s/openvpn-user-management.sock".formatted(
                folderFactory.getOpenVpnRunDir()
        );
    }

    public OpenVpnManagementIf getUserManagement() {
        return userManagementIf;
    }

    public OpenVpnManagementIf getSiteManagement() {
        return siteManagementIf;
    }

    public void wakeUp() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        log.info("onApplicationEvent");
    }
}
