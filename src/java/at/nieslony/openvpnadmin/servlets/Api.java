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

import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.beans.FirewallSettings;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Claas Nieslony
 */
public class Api extends HttpServlet {
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private FacesContext getFacesContext(HttpServletRequest request, HttpServletResponse response) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {

            FacesContextFactory contextFactory  =
                (FacesContextFactory)FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
            LifecycleFactory lifecycleFactory =
                (LifecycleFactory)FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            Lifecycle lifecycle =
                lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);

            facesContext =
                contextFactory.getFacesContext(request.getSession().getServletContext(),
                    request, response, lifecycle);
        }

        return facesContext;
    }

    private <T> T getBean(FacesContext ctx, String beanName, Class<? extends T> expectedType)
            throws javax.el.ELException
    {
        String elExpression = String.format("#{%s}", beanName);
        return ctx.getApplication().evaluateExpressionGet(ctx, elExpression, expectedType);
    }

    private void requireUser(HttpServletRequest request, HttpServletResponse response)
            throws AccessDeniedException, ELException
    {
        FacesContext fCtx = getFacesContext(request, response);
        CurrentUser currentUser = getBean(fCtx, "currentUser", CurrentUser.class);

        if (currentUser == null ||
                !currentUser.isValid() ||
                !currentUser.hasRole("user")
                ) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private int handleAuth(LinkedList<String> subCommands,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException
    {
        requireUser(request, response);

        return HttpServletResponse.SC_OK;
    }

    private int handleFirewall(LinkedList<String> subCommands,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException
    {
        requireUser(request, response);

        FacesContext fCtx = getFacesContext(request, response);
        CurrentUser currentUser = getBean(fCtx, "currentUser", CurrentUser.class);
        FirewallSettings firewallSettings = getBean(fCtx, "firewallSettings", FirewallSettings.class);

        try (PrintWriter out = response.getWriter()) {
            out.println(firewallSettings.getFirewallConfig(currentUser.getUser()));
        }

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
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String pathInfo = request.getPathInfo();
        String command = null;
        LinkedList<String> apiPath = null;
        if (pathInfo != null && !pathInfo.isEmpty()) {
            apiPath = new LinkedList<>(Arrays.asList(request.getPathInfo().split("/")));
            apiPath.pop();
            command = apiPath.pop();
        }

        int status = -1;

        PrintWriter out = response.getWriter();
        try {
            if (command == null || apiPath == null) {
                out.println("Illegal API call");
                status = HttpServletResponse.SC_NOT_FOUND;
            }
            else if (command.equals("firewall"))
                status = handleFirewall(apiPath, request, response);
            else if (command.equals("auth"))
                status = handleAuth(apiPath, request, response);
            else {
                out.println("Illegal API call");
                status = HttpServletResponse.SC_NOT_FOUND;
            }
        }
        catch (AccessDeniedException ex) {
            logger.warning(ex.getMessage());
            status = HttpServletResponse.SC_FORBIDDEN;
            out.println("Forbidden");
        }
        catch (Exception ex) {
            logger.warning(String.format("Cannot execute library call %s: %s",
                    pathInfo, ex.getMessage()));
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        out.close();

        response.setStatus(status);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
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
