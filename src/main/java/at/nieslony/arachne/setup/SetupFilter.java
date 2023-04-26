/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.setup;

/**
 *
 * @author claas
 */
public class SetupFilter {
}
/*
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SetupFilter implements Filter {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(SetupFilter.class);

    @Autowired
    private SettingsRepository settingsRepository;

    @Override
    public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) sr;
        HttpServletResponse response = (HttpServletResponse) sr1;

        Optional<SettingsModel> settingsModel
                = settingsRepository.findBySetting(SetupController.SETUP_STATUS_KEY);
        String uri = request.getRequestURI();

        if (uri.startsWith("/public/")) {
            fc.doFilter(sr, sr1);
        } else if (settingsModel.isEmpty()) {
            if (uri.equals("/setup")) {
                logger.info("Let's setup arachne");
                fc.doFilter(sr, sr1);
            } else {
                logger.info("No setup status (%s) -> redirecting to setup page".formatted(uri));
                response.sendRedirect("/setup");
            }
        } else {
            if (uri.equals("/setup")) {
                logger.info("Setup is already done. Returning NOT_FOUND");
                response.sendError(HttpStatus.NOT_FOUND.value());
            } else {
                // logger.info("Setup already finished. Let's go");
                fc.doFilter(request, response);
            }
        }
    }
}
 */
