/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

// https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer
import at.nieslony.arachne.auth.LoginOrSetupView;
import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.auth.token.BearerTokenAuthFilter;
import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.InternalUserDetailsService;
import at.nieslony.arachne.users.LdapUserDetailsService;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
import org.springframework.security.web.SecurityFilterChain;
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
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
@Slf4j
public class SecurityConfiguration {

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
            log.debug(kerberosSettings.toString());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = authManager(http);
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
                .httpBasic((b) -> b.realmName("Arachne"))
                .addFilterAfter(
                        spnegoAuthenticationProcessingFilter(authenticationManager),
                        BasicAuthenticationFilter.class)
                .addFilterAfter(
                        requestAttributeAuthenticationFilter(authenticationManager),
                        BasicAuthenticationFilter.class
                )
                .exceptionHandling(
                        (exceptions) -> {
                            if (kerberosSettings.isEnableKrbAuth()) {
                                exceptions.authenticationEntryPoint(
                                        spnegoEntryPoint()
                                );
                            }
                        }
                );

        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginOrSetupView.class, "/arachne/login");
        }).build();
    }

    @Bean
    public Filter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        if (kerberosSettings.isEnableKrbAuth()) {
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
        } else {
            return (ServletRequest sr, ServletResponse sr1, FilterChain fc) -> {
                fc.doFilter(sr, sr1);
            };
        }
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

    @Bean
    public PreAuthenticatedAuthenticationProvider createPreAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService((token) -> {
            log.info("Get user details from pre auth token for : " + token.getName());
            return ldapUserDetailsService.loadUserByUsername(token.getName());
        });

        return provider;
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
                log.info("Authenticated with REMOTE_USER as " + authentication.getPrincipal().toString());
            });
            filter.setAuthenticationFailureHandler((request, response, exception) -> {
                log.warn("Authentication with REMOTE_USER failed: " + exception.getMessage());
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
}
