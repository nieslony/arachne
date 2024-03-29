/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

import at.nieslony.arachne.auth.LoginOrSetupView;
import at.nieslony.arachne.auth.PreAuthSettings;
import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.ArachneUserDetailsService;
import at.nieslony.arachne.utils.FolderFactory;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

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

    private static final String ERROR_401
            = """
            <html>
                <head>
                    <meta http-equiv="refresh" content="0; URL=/arachne/login" />
                </head>
                <body>
                    Redirecting to login page...
                </body>
            </html>
            """;

    public enum UnAuthorizedHandler {
        None,
        Error401,
        FormLogin
    }

    public static String getUnAuthoAttr() {
        return UnAuthorizedHandler.class.getName();
    }

    @Autowired
    private Settings settings;

    @Autowired
    private ArachneUserDetailsService arachneUserDetailsService;

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
        SpnegoEntryPoint sep = new SpnegoEntryPoint("/unauthorized");
        return sep;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

        super.configure(http);
        setLoginView(http, LoginOrSetupView.class, "/arachne/login");

        if (preAuthSettings.isPreAuthtEnabled()) {
            http.addFilter(requestAttributeAuthenticationFilter(authenticationManager));
        }
        http.exceptionHandling((eh) -> {
            eh.authenticationEntryPoint(spnegoEntryPoint());
        });
        if (kerberosSettings.isEnableKrbAuth()) {
            http.addFilterBefore(spnegoAuthenticationProcessingFilter(authenticationManager),
                    BasicAuthenticationFilter.class);
            /*http.addFilterBefore(
                    (req, res, fc) -> {
                        HttpServletRequest httpRequest = (HttpServletRequest) req;
                        HttpServletResponse httpResponse = (HttpServletResponse) res;
                        var session = httpRequest.getSession(true);
                        logger.info("[%s] %s %s"
                                .formatted(
                                        httpRequest.getMethod(),
                                        session.getId(),
                                        httpRequest.getServletPath()
                                )
                        );
                        var userPrincipal = httpRequest.getUserPrincipal();
                        if (session.getAttribute(getUnAuthoAttr()) == null) {
                            session.setAttribute(
                                    getUnAuthoAttr(),
                                    UnAuthorizedHandler.None
                            );
                        }
                        if (userPrincipal == null
                        && !httpRequest.getServletPath().equals("/login")
                        && session.getAttribute(getUnAuthoAttr()) == UnAuthorizedHandler.None) {
                            var writer = httpResponse.getWriter();
                            writer.println(ERROR_401);
                            httpResponse.addHeader("WWW-Authenticate", "Negotiate");
                            httpResponse.setContentType("text/html; charset=iso-8859-1");
                            httpResponse.setStatus(401);
                            session.setAttribute(
                                    getUnAuthoAttr(),
                                    UnAuthorizedHandler.Error401
                            );
                            logger.info("Error 401");
                        } else {
                            if (userPrincipal != null) {
                                logger.info("Authtenticated as " + userPrincipal.getName());
                            } else {
                                logger.info("Not yet authenticated");
                            }
                            fc.doFilter(req, res);
                            try {
                                if (userPrincipal != null
                                && userPrincipal instanceof KerberosServiceRequestToken ksrt
                                && session.getAttribute(getUnAuthoAttr()) == UnAuthorizedHandler.Error401) {
                                    try {
                                        session.setAttribute(
                                                getUnAuthoAttr(),
                                                UnAuthorizedHandler.None
                                        );
                                        logger.info("AuthHandler -> None");
                                    } catch (IllegalStateException ex) {
                                        logger.info("Session already invalidated");
                                    }
                                }
                            } catch (IllegalStateException ex) {
                                logger.info("Session already invalidated");
                            }
                        }
                    },
                    AuthorizationFilter.class
            );*/
        }
    }

    @Bean
    public Filter requestAttributeAuthenticationFilter(
            AuthenticationManager authenticationManager
    ) {
        if (preAuthSettings.isPreAuthtEnabled()) {
            RequestAttributeAuthenticationFilter filter = new RequestAttributeAuthenticationFilter();
            filter.setExceptionIfVariableMissing(false);
            filter.setPrincipalEnvironmentVariable(preAuthSettings.getEnvironmentVariable());
            filter.setAuthenticationManager(authenticationManager);
            filter.setAuthenticationSuccessHandler((request, response, authentication) -> {
                logger.info("Authenticated with REMOTE_USER as " + authentication.getPrincipal().toString());
            });
            filter.setAuthenticationFailureHandler((request, response, exception) -> {
                logger.warn("Authentication with REMOTE_USER failed: " + exception.getMessage());
            });
            filter.setAuthenticationDetailsSource((context) -> {
                return arachneUserDetailsService;
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
                response.sendError(HttpStatus.UNAUTHORIZED.value());
            });
            filter.setSuccessHandler((request, response, authentication) -> {
                logger.info("Access to %s granted".formatted(request.getContextPath()));
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

        return authManBuilder.build();
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService((token) -> {
            logger.info("Get user details from pre auth token for : " + token.getName());
            return arachneUserDetailsService.loadUserByUsername(token.getName());
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
        provider.setUserDetailsService(arachneUserDetailsService);
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
        provider.setUserDetailsService(arachneUserDetailsService);
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
