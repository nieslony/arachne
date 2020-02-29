/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.servlets;

import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author claas
 */
abstract public class AbstractFacesServlet extends HttpServlet {
    protected static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    CurrentUser currentUser;

    public AbstractFacesServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected abstract void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException;

    /** Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
    */
@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse
        response) throws ServletException, IOException
    {
        FacesContext ctx = getFacesContext(request, response);

        if (currentUser == null) {
            logger.warning("There is no current ujser");
        }
        else if (!currentUser.isValid()) {
            logger.warning("Current user is not valid");
        }
        else
            try {
                processRequest(request, response);
            }
            catch (PermissionDenied ex) {
                ExternalContext ectx = ctx.getExternalContext();
                ectx.setResponseStatus(HttpServletResponse.SC_FORBIDDEN);
                Map<String, Object> requestMap = ectx.getRequestMap();
                requestMap.put("errorMsg", ex.getMessage());
                String errPage = "/error/error.xhtml";
                logger.warning(ex.getMessage());
                ectx.dispatch(errPage);
            }

        try {
            ctx.release();
        }
        catch (IllegalStateException ex) {
            logger.log(Level.INFO, "Cannot release faces context:{0}", ex.getMessage());
        }
    }

    protected void log(FacesContext facesContext, String message) {
        facesContext.getExternalContext().log(message);
    }
    /** Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
    */
@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse
        response) throws ServletException, IOException {
        processRequest(request, response);
    }
    protected FacesContext getFacesContext(HttpServletRequest request,
        HttpServletResponse response) {
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

            // Set using our inner class

            InnerFacesContext.setFacesContextAsCurrentInstance(facesContext);

            // set a new viewRoot, otherwise context.getViewRoot returns null
            UIViewRoot view =
            facesContext.getApplication().getViewHandler().createView(facesContext, "");
            facesContext.setViewRoot(view);
        }
        return facesContext;
    }
    public void removeFacesContext() {
        InnerFacesContext.setFacesContextAsCurrentInstance(null);
    }
    protected Application getApplication(FacesContext facesContext) {
        return facesContext.getApplication();
    }
    protected Object getManagedBean(String beanName, FacesContext
        facesContext) {
    return
        getApplication(facesContext).getVariableResolver().resolveVariable(facesContext,
            beanName);
    }
    // You need an inner class to be able to call FacesContext.setCurrentInstance
    // since it's a protected method
    private abstract static class InnerFacesContext extends FacesContext {
        protected static void setFacesContextAsCurrentInstance(FacesContext
            facesContext) {
            FacesContext.setCurrentInstance(facesContext);
        }
    }
}
