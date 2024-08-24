/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

import at.nieslony.arachne.auth.LoginOrSetupView;
import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.auth.token.BearerAuthenticationProvider;
import at.nieslony.arachne.auth.token.BearerTokenAuthFilter;
import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.InternalUserDetailsService;
import at.nieslony.arachne.users.LdapUserDetailsService;
import at.nieslony.arachne.utils.FolderFactory;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
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
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
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
public class SecurityConfiguration extends VaadinWebSecurity {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Autowired
    private Settings settings;

    @Autowired
    private InternalUserDetailsService internalUserDetailsService;

    @Autowired
    private LdapUserDetailsService ldapUserDetailsService;

    @Autowired
    BearerAuthenticationProvider bearerAuthenticationProvider;

    @Autowired
    BearerTokenAuthFilter bearerTokenAuthFilter;

    @Autowired
    private FolderFactory folderFactory;

    private KerberosSettings kerberosSettings;
    private PreAuthSettings preAuthSettings;

    @PostConstruct
    public void init() {
        kerberosSettings = settings.getSettings(KerberosSettings.class);
        preAuthSettings = settings.getSettings(PreAuthSettings.class);
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        SpnegoEntryPoint sep = new SpnegoEntryPoint("/login");
        return sep;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = http.getSharedObject(
                AuthenticationManager.class
        );

        super.configure(http);
        setLoginView(http, LoginOrSetupView.class, "/arachne/login");
        http
                .httpBasic((t) -> {
                    t.realmName("Arachne");
                }).addFilterBefore(
                bearerTokenAuthFilter,
                BasicAuthenticationFilter.class
        );

        if (preAuthSettings.isPreAuthtEnabled()) {
            http.addFilter(requestAttributeAuthenticationFilter(authenticationManager));
        }
        if (kerberosSettings.isEnableKrbAuth()) {
            http
                    .addFilterBefore(spnegoAuthenticationProcessingFilter(authenticationManager),
                            BasicAuthenticationFilter.class
                    )
                    .exceptionHandling(
                            (exceptions) -> exceptions
                                    .authenticationEntryPoint(
                                            spnegoEntryPoint()
                                    )
                    );
        }
    }

    @Bean
    public Filter requestAttributeAuthenticationFilter(
            AuthenticationManager authenticationManager
    ) {
        if (preAuthSettings.isPreAuthtEnabled()) {
            var filter = switch (preAuthSettings.getPreAuthSource()) {
                case ENVIRONMENT_VARIABLE -> {
                    RequestAttributeAuthenticationFilter envFilter = new RequestAttributeAuthenticationFilter();
                    envFilter.setExceptionIfVariableMissing(false);
                    envFilter.setPrincipalEnvironmentVariable(preAuthSettings.getEnvironmentVariable());
                    yield envFilter;
                }
                case HTTP_HEADER -> {
                    RequestHeaderAuthenticationFilter headerFilter = new RequestHeaderAuthenticationFilter();
                    headerFilter.setExceptionIfHeaderMissing(false);
                    headerFilter.setPrincipalRequestHeader(preAuthSettings.getHttpHeader());
                    yield headerFilter;
                }
            };
            filter.setAuthenticationManager(authenticationManager);
            filter.setAuthenticationSuccessHandler((request, response, authentication) -> {
                logger.info("Authenticated with REMOTE_USER as " + authentication.getPrincipal().toString());
            });
            filter.setAuthenticationFailureHandler((request, response, exception) -> {
                logger.warn("Authentication with REMOTE_USER failed: " + exception.getMessage());
            });
            filter.setAuthenticationDetailsSource((context) -> {
                return ldapUserDetailsService;
            });
            return filter;
        } else {
            return (ServletRequest sr, ServletResponse sr1, FilterChain fc) -> {
                fc.doFilter(sr, sr1);
            };
        }
    }

    @Bean
    public Filter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager
    ) {
        if (kerberosSettings.isEnableKrbAuth()) {
            logger.info("Creating SpnegoAuthenticationProcessingFilter");
            SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
            filter.setAuthenticationManager(authenticationManager);
            filter.setFailureHandler((request, response, exception) -> {
                logger.error("Cannot authenticate with Kerberos: " + exception.getMessage());
                response.setHeader("WWW-Authenticate", "Negotiate");
                response.sendError(HttpStatus.UNAUTHORIZED.value());
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
        } else {
            return (ServletRequest sr, ServletResponse sr1, FilterChain fc) -> {
                fc.doFilter(sr, sr1);
            };
        }
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Customize your WebSecurity configuration.
        super.configure(web);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        logger.info("Creating AuthenticationManager");

        var authManBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        if (kerberosSettings.isEnableKrbAuth()) {
            authManBuilder
                    .authenticationProvider(kerberosAuthenticationProvider())
                    .authenticationProvider(kerberosServiceAuthenticationProvider());
        }
        if (preAuthSettings.isPreAuthtEnabled()) {
            authManBuilder.authenticationProvider(preAuthenticatedAuthenticationProvider());
        }
        authManBuilder.authenticationProvider(bearerAuthenticationProvider);
        authManBuilder.userDetailsService(internalUserDetailsService);
        authManBuilder.eraseCredentials(true);
        authManBuilder.parentAuthenticationManager(null);

        return authManBuilder.build();
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService((token) -> {
            logger.info("Get user details from pre auth token for : " + token.getName());
            return ldapUserDetailsService.loadUserByUsername(token.getName());
        });

        return provider;
    }

    @Bean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        logger.info("Creating kerberosAuthenticationProvider");
        if (!kerberosSettings.isEnableKrbAuth()) {
            return null;
        }
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        SunJaasKerberosClient client = new SunJaasKerberosClient();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(ldapUserDetailsService);
        return provider;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        logger.info("Creating kerberosServiceAuthenticationProvider");
        if (!kerberosSettings.isEnableKrbAuth()) {
            return null;
        }
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator());
        provider.setUserDetailsService(ldapUserDetailsService);
        return provider;
    }

    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        logger.info("Creating sunJaasKerberosTicketValidator");
        if (!kerberosSettings.isEnableKrbAuth()) {
            return null;
        }

        SunJaasKerberosTicketValidator ticketValidator = new SunJaasKerberosTicketValidator();
        ticketValidator.setServicePrincipal(kerberosSettings.getServicePrincipal());
        ticketValidator.setKeyTabLocation(
                new FileSystemResource(
                        kerberosSettings.getKeytabPath()
                )
        );
        return ticketValidator;
    }
}
