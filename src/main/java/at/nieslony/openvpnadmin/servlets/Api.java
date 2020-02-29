/*
 * Copyright (C) 2018 Claas Nieslony
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
package at.nieslony.openvpnadmin.servlets;

import at.nieslony.openvpnadmin.beans.FirewallSettings;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Claas Nieslony
 */
@WebServlet("/api/*")
public class Api extends AbstractFacesServlet {
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    FirewallSettings firewallSettings;

    private int handleAuth(LinkedList<String> subCommands,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException
    {
        if (!currentUser.hasRole("user"))
            throw new PermissionDenied();

        return HttpServletResponse.SC_OK;
    }

    private int handleFirewall(LinkedList<String> subCommands,
            HttpServletRequest request,
            HttpServletResponse response,
            PrintWriter out)
            throws ServletException, IOException
    {
        if (!currentUser.hasRole("user"))
            throw new PermissionDenied();

        String jsonStr = firewallSettings.getFirewallConfig(currentUser.getUser());
        logger.info(jsonStr);
        out.println(jsonStr);
        out.flush();

        return HttpServletResponse.SC_OK;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        String command = null;
        LinkedList<String> apiPath = null;
        if (pathInfo != null && !pathInfo.isEmpty()) {
            apiPath = new LinkedList<>(Arrays.asList(request.getPathInfo().split("/")));
            apiPath.pop();
            command = apiPath.pop();
        }

        int status;

        try (PrintWriter out = response.getWriter()) {
            try {
                if (command == null || apiPath == null) {
                    out.println("Illegal API call");
                    status = HttpServletResponse.SC_NOT_FOUND;
                }
                else if (command.equals("firewall")) {
                    status = handleFirewall(apiPath, request, response, out);
                    response.setContentType("application/json");
                }
                else if (command.equals("auth"))
                    status = handleAuth(apiPath, request, response);
                else {
                    out.println("Illegal API call");
                    status = HttpServletResponse.SC_NOT_FOUND;
                }
            }
            catch (IOException | ServletException ex) {
                logger.warning(String.format("Cannot execute library call %s: %s",
                        pathInfo, ex.getMessage()));
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
        }

        response.setStatus(status);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
