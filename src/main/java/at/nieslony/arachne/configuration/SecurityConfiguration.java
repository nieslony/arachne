/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

import at.nieslony.arachne.auth.LoginOrSetupView;
import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.auth.token.BearerTokenAuthFilter;
import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.InternalUserDetailsService;
import at.nieslony.arachne.users.LdapUserDetailsService;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 *
 * @author claas
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
@Slf4j
public class SecurityConfiguration extends VaadinWebSecurity {

    @Autowired
    private Settings settings;

    @Autowired
    private InternalUserDetailsService internalUserDetailsService;

    @Autowired
    private LdapUserDetailsService ldapUserDetailsService;

    @Autowired
    BearerTokenAuthFilter bearerTokenAuthFilter;

    private KerberosSettings kerberosSettings;
    private PreAuthSettings preAuthSettings;

    @PostConstruct
    public void init() {
        log.info("Initializing...");
        try {
            kerberosSettings = settings.getSettings(KerberosSettings.class);
            preAuthSettings = settings.getSettings(PreAuthSettings.class);
            log.info(kerberosSettings.toString());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        log.info("Configure");

        http
                .exceptionHandling((t) -> {
                    t.accessDeniedPage("/");
                })
                .userDetailsService(internalUserDetailsService)
                .userDetailsService(ldapUserDetailsService)
                .addFilterBefore(
                        bearerTokenAuthFilter,
                        BasicAuthenticationFilter.class
                )
                .httpBasic((b) -> {
                    b.realmName("IMAP Admin");

                });

        AuthenticationManager authenticationManager = authManager(http);
        if (kerberosSettings.isEnableKrbAuth()) {
            http
                    .addFilterAfter(
                            spnegoAuthenticationProcessingFilter(authenticationManager),
                            BasicAuthenticationFilter.class)
                    .exceptionHandling(
                            (exceptions) -> exceptions
                                    .authenticationEntryPoint(
                                            spnegoEntryPoint()
                                    )
                    );
        }

        super.configure(http);
        setLoginView(http, LoginOrSetupView.class, "/arachne/login");
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager);
        filter.setFailureHandler((request, response, exception) -> {
            log.error("Access to %s failed: %s"
                    .formatted(request.getPathInfo(), exception.getMessage())
            );
        });
        filter.setSuccessHandler(new SavedRequestAwareAuthenticationSuccessHandler() {
            private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

            private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

            @Override
            public void onAuthenticationSuccess(
                    final HttpServletRequest request,
                    final HttpServletResponse response,
                    final Authentication authentication
            ) throws IOException, ServletException {
                logger.info("Access to %s granted".formatted(request.getContextPath()));
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
            }
        });

        return filter;
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(kerberosAuthenticationProvider())
                .authenticationProvider(kerberosServiceAuthenticationProvider());
        authBuilder.userDetailsService(internalUserDetailsService);
        authBuilder.parentAuthenticationManager(null);

        return authBuilder.build();
    }

    @Bean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        log.info("Create KerberosAuthenticationProvider");
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        SunJaasKerberosClient client = new SunJaasKerberosClient();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(ldapUserDetailsService);
        return provider;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator());
        provider.setUserDetailsService(ldapUserDetailsService);
        return provider;
    }

    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        SunJaasKerberosTicketValidator ticketValidator = new SunJaasKerberosTicketValidator();
        ticketValidator.setServicePrincipal(
                kerberosSettings.getServicePrincipal()
        );
        ticketValidator.setKeyTabLocation(
                new FileSystemResource(kerberosSettings.getKeytabPath())
        );
        return ticketValidator;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
