/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.configuration;

import at.nieslony.arachne.auth.LoginOrSetupView;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 *
 * @author claas
 */
@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Autowired
    private Settings settings;

    @Autowired
    private ArachneUserDetailsService arachneUserDetailsService;

    /*@Autowired
    private WwwAuthenticateFilter wwwAuthenticateFilter;*/
    @Autowired
    private FolderFactory folderFactory;

    private KerberosSettings kerberosSettings;

    @PostConstruct
    public void init() {
        kerberosSettings = settings.getSettings(KerberosSettings.class);
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        SpnegoEntryPoint sep = new SpnegoEntryPoint("/login");
        return sep;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth
                -> auth.requestMatchers(
                        AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/images/*.png")).permitAll());

        super.configure(http);
        setLoginView(http, LoginOrSetupView.class);

        if (kerberosSettings.isEnableKrbAuth()) {
            AuthenticationManager authenticationManager
                    = http.getSharedObject(AuthenticationManager.class);

            http
                    .exceptionHandling(
                            eh -> eh.authenticationEntryPoint(
                                    spnegoEntryPoint()
                            )
                    )
                    .authenticationProvider(
                            kerberosAuthenticationProvider()
                    )
                    .authenticationProvider(
                            kerberosServiceAuthenticationProvider()
                    )
                    .exceptionHandling((t) -> {
                        t.accessDeniedPage("/arachne/login");
                    })
                    .addFilterBefore(
                            spnegoAuthenticationProcessingFilter(authenticationManager),
                            BasicAuthenticationFilter.class
                    );
        } else {
            logger.info(
                    "Kerberos is disabled, don't add authentication providers and filters"
            );
        }
    }

    /*@Override
    protected void ___configure(HttpSecurity http) throws Exception {
        http
                .csrf((csrf)
                        -> csrf.disable())
                .headers((headers)
                        -> headers.frameOptions((fro) -> fro.disable()))
                .httpBasic((configurator)
                        -> configurator.realmName("Arachne openVPN Administrator")
                );
        super.configure(http);
        setLoginView(http, LoginOrSetupView.class, "/arachne/login");

        if (kerberosSettings.isEnableKrbAuth()) {
            AuthenticationManager authenticationManager
                    = http.getSharedObject(AuthenticationManager.class);

            http
                    .exceptionHandling(
                            eh -> eh.authenticationEntryPoint(
                                    spnegoEntryPoint()
                            )
                    )
                    .authenticationProvider(
                            kerberosAuthenticationProvider()
                    )
                    .authenticationProvider(
                            kerberosServiceAuthenticationProvider()
                    )
                    .addFilterAfter(
                            spnegoAuthenticationProcessingFilter(authenticationManager),
                            SessionManagementFilter.class
                    ) exceptionHandling(
                            (t) -> {

                                logger.info("Access denied. Forward to login page");
                                //t.accessDeniedPage("/arachne/login");
                            })*/ /*.addFilterBefore(
                            wwwAuthenticateFilter,
                            UsernamePasswordAuthenticationFilter.class
                    );;

        } else {
            logger.info(
                    "Kerberos is disabled, don't add authentication providers and filters"
            );
        }
    }
     */
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
//        web.ignoring().requestMatchers("/unauthorized");
//        web.ignoring().requestMatchers("/unauthorized");
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

        return authManBuilder.build();
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
