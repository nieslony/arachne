/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.servlets;

import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author claas
 */
@WebServlet(name = "Download", urlPatterns = {"/download/*"})
public class Download extends AbstractFacesServlet {
    @Inject
    ConfigBuilder configBuilder;

    private void handleNotFound(HttpServletRequest request, HttpServletResponse response, String fileName) {
        try (PrintWriter printWriter = response.getWriter()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain");
            printWriter.write(String.format("Download not found: %s\n", fileName));
        }
        catch (IOException ex) {
        }
    }

    private void handleAdddVpnToNetworkManagerSh(HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        response.setContentType("text/x-shellscript");
        try (PrintWriter printWriter = response.getWriter()) {
            configBuilder.writeUserVpnNetworkManagerConfig(printWriter, currentUser.getUsername());
        }
    }

    private void handleClientConfig(HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        response.setContentType("text/x-shellscript");
        try (PrintWriter printWriter = response.getWriter()) {
            configBuilder.writeUserVpnClientConfig(printWriter, currentUser.getUsername());
        }
    }

    private void handleIndex(HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        logger.info("Getting index");
        String index = "<empty>";

        ServletContext sctx = request.getServletContext();

        try (InputStream is = sctx.getResourceAsStream("/WEB-INF/download-index.html")) {
            index = IOUtils.toString(is, StandardCharsets.UTF_8);
            logger.info(index);
        }
        catch (IOException ex) {
            logger.warning(ex.getMessage());
        }

        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.write(index);
        }
    }
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     */
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        if (!currentUser.hasRole("user")) {
            String msg = String.format("User %s doesn't have role user", currentUser.getUsername());
            throw new PermissionDenied(msg);
        }

        String pathInfo = request.getPathInfo();
        String[] splitPath = null;
        if (pathInfo != null && !pathInfo.isEmpty()) {
            splitPath = request.getPathInfo().split("/");
        }

        String fileName = "";
        if (splitPath != null && splitPath.length >= 2) {
            fileName = splitPath[1];
        }
        try {
            logger.info(String.format("Download: %s", fileName));
            switch (fileName) {
                case "add-vpn-to-networkmanager.sh":
                    handleAdddVpnToNetworkManagerSh(request, response);
                    break;
                case "client-config.ovpn":
                    handleClientConfig(request, response);
                    break;
                case "":
                    handleIndex(request, response);
                    break;
                default:
                    handleNotFound(request, response, fileName);
            }
        }
        catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot handle download: {0}", ex.getMessage());
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
