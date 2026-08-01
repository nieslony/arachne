/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

// https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer
import at.nieslony.arachne.auth.LoginOrSetupView;
import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.auth.TotpController;
import at.nieslony.arachne.auth.token.BearerTokenAuthFilter;
import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.onetimeview.OneTimeViewModel;
import at.nieslony.arachne.onetimeview.OneTimeViewRepository;
import at.nieslony.arachne.openvpn.OpenVpnService;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.ArachneUserDetails;
import at.nieslony.arachne.users.InternalUserDetailsService;
import at.nieslony.arachne.users.LdapUserDetailsService;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.router.NotFoundException;
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
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
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
@EnableWebSecurity(debug = false)
@EnableMethodSecurity(jsr250Enabled = true)
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

    @Autowired
    OpenVpnService openVpnController;

    @Autowired
    TotpController totpController;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OneTimeViewRepository oneTimeViewRepository;

    private KerberosSettings kerberosSettings;
    private PreAuthSettings preAuthSettings;

    private static final Pattern OTV_LANDING_PATTERN
            = Pattern.compile("/otv/(?<id>[0-9a-fA-F]+)");
    private static final Pattern OTV_PAGE_PATTERN
            = Pattern.compile("/otv/(?<id>[0-9a-fA-F]+)/(?<page>[a-z0-9\\-]+)");

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
    @Order(10)
    public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = authManager(http);
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PathRequest.toStaticResources()
                            .atCommonLocations()).permitAll();
                    auth.requestMatchers("/icons/**").permitAll();
                    auth.requestMatchers("/theme/**").permitAll();
                    auth.requestMatchers("/otv/**").access(otvAuthManager());
                })
                .csrf(
                        (t) -> {
                            t.ignoringRequestMatchers("/api/**");

                        }
                )
                .userDetailsService(internalUserDetailsService)
                .userDetailsService(ldapUserDetailsService)
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
                                        uiSpnegoEntryPoint()
                                );
                            }
                        }
                );

        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginOrSetupView.class, "/arachne/login");
            configurer.enableCsrfConfiguration(true);
        }).build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = authManager(http);
        return http.securityMatcher("/api/**")
                .userDetailsService(internalUserDetailsService)
                .userDetailsService(ldapUserDetailsService)
                .addFilterBefore(
                        bearerTokenAuthFilter,
                        BasicAuthenticationFilter.class
                )
                .httpBasic((b) -> b.realmName("Arachne"))
                .addFilterBefore(
                        otpAuthenticationFilter(),
                        AuthorizationFilter.class
                )
                .addFilterAfter(
                        spnegoAuthenticationProcessingFilter(authenticationManager),
                        BasicAuthenticationFilter.class
                )
                .addFilterAfter(
                        requestAttributeAuthenticationFilter(authenticationManager),
                        BasicAuthenticationFilter.class
                )
                .exceptionHandling(
                        (exceptions) -> {
                            if (kerberosSettings.isEnableKrbAuth()) {
                                exceptions.authenticationEntryPoint(
                                        apiSpnegoEntryPoint()
                                );
                            }
                        }
                )
                .build();
    }

    public Filter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        if (kerberosSettings.isEnableKrbAuth()) {
            SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
            filter.setAuthenticationManager(authenticationManager);
            filter.setFailureHandler((request, response, exception) -> {
                log.error("Access to %s failed: %s"
                        .formatted(
                                request.getPathTranslated(),
                                exception.getMessage()
                        )
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
                    log.info("Access to %s granted".formatted(
                            request.getPathTranslated())
                    );
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
    public SpnegoEntryPoint uiSpnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }

    @Bean
    public SpnegoEntryPoint apiSpnegoEntryPoint() {
        return new SpnegoEntryPoint("/api/error/401");
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

    public Filter otpAuthenticationFilter() {
        return (ServletRequest sreq, ServletResponse sresp, FilterChain fc) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) sreq;
            HttpServletResponse httpResponse = (HttpServletResponse) sresp;

            if (httpRequest.getUserPrincipal() instanceof UsernamePasswordAuthenticationToken authToken) {
                ArachneUserDetails userDetails
                        = (ArachneUserDetails) authToken.getPrincipal();
                UserModel user = userRepository.findByUsername(
                        userDetails.getUsername()
                );
                String username = user.getUsername();
                if (openVpnController.isOtpRequired(user)) {
                    String otp = httpRequest.getHeader("X-OTP");
                    if (otp == null) {
                        log.error(
                                "OTP for user %s required but not supplied in header X-OTP"
                                        .formatted(username)
                        );
                        createUnauthorized(httpResponse);
                        return;
                    } else {
                        if (!totpController.validateTotp(otp, user)) {
                            log.error("User %s supplied invalid TOTP".formatted(username));
                            createUnauthorized(httpResponse);
                            return;
                        }
                        log.info("OTP for user %s is valid".formatted(username));
                    }
                } else {
                    log.info("OTP not required for user " + username);
                }
            }
            fc.doFilter(sreq, sresp);
        };
    }

    private void createUnauthorized(HttpServletResponse resp) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        resp.getOutputStream().println("%d %s\n".formatted(
                status.value(),
                status.getReasonPhrase()
        ));
        resp.setStatus(status.value());
    }

    AuthorizationManager<RequestAuthorizationContext> otvAuthManager() {
        return new AuthorizationManager<RequestAuthorizationContext>() {
            @Override
            public AuthorizationResult authorize(
                    Supplier authSupplier,
                    RequestAuthorizationContext rac
            ) {
                HttpServletRequest req = rac.getRequest();
                String path = req.getServletPath();

                Matcher otvLandingMatcher = OTV_LANDING_PATTERN.matcher(
                        path
                );
                if (otvLandingMatcher.matches()) {
                    log.info("Access to landing page %s is granted".formatted(path));
                    return new AuthorizationDecision(true);
                }

                Matcher otvPageMatcher = OTV_PAGE_PATTERN.matcher(
                        path
                );
                if (otvPageMatcher.matches()) {
                    OneTimeViewModel model
                            = oneTimeViewRepository
                                    .findById(otvPageMatcher.group("id"))
                                    .orElseThrow(NotFoundException::new);
                    if (model.getVisited() != null) {
                        log.error("OTV %s already visited at %s ".formatted(
                                model.getId(),
                                model.getVisitedString()
                        ));
                        return new AuthorizationDecision(false);
                    }
                    if (LocalDateTime.now().isAfter(model.getValidUntil())) {
                        log.error("OTV %s is expired since ".formatted(
                                model.getId(),
                                model.getValidUntilString()
                        ));
                        return new AuthorizationDecision(false);
                    }

                    boolean granted;
                    if (authSupplier.get() instanceof AbstractAuthenticationToken user) {
                        granted = user.getName().equals(model.getUsername());
                        log.info(
                                "Required username: %s authenticated as %s: access granted: %b"
                                        .formatted(
                                                model.getUsername(),
                                                user.getName(),
                                                granted
                                        )
                        );
                    } else {
                        log.info(
                                "Unexpected Authentication class: "
                                + authSupplier.get().getClass().getName()
                        );
                        granted = false;
                    }
                    log.info("Access to page %s: %b: ".formatted(
                            otvPageMatcher.group("page"),
                            granted
                    ));
                    return new AuthorizationDecision(granted);
                }

                log.info("Path %s does not match".formatted(path));
                return new AuthorizationDecision(true);
            }
        };
    }
}
