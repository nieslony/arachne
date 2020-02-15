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

package at.nieslony.openvpnadmin.errorhandling;

import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author claas
 */
public class ExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger LOG = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final javax.faces.context.ExceptionHandler wrapped;

    public ExceptionHandler(final javax.faces.context.ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public javax.faces.context.ExceptionHandler getWrapped() {
        return this.wrapped;
    }

    @Override
    public void handle() {
               final Iterator<ExceptionQueuedEvent> queue = getUnhandledExceptionQueuedEvents().iterator();

        while (queue.hasNext()){
            ExceptionQueuedEvent item = queue.next();
            ExceptionQueuedEventContext exceptionQueuedEventContext = (ExceptionQueuedEventContext)item.getSource();

            try {
                Throwable throwable = exceptionQueuedEventContext.getException();
                Throwable rootCause = getRootCause(throwable);
                Throwable cause = throwable;
                if (cause != null)
                    while (cause.getCause() != null)
                        cause = cause.getCause();

                LOG.severe("--- Caught exception ---");
                LOG.severe(String.format("Throwable: message: %s", throwable.getMessage()));
                LOG.severe(String.format("Throwable: %s", throwable.getClass().getName()));

                String rcName = "unknown";
                if (rootCause != null) {
                    rcName = rootCause.getClass().getName();
                    LOG.severe(String.format("Root cause: %s", rootCause.getClass().getName()));
                }
                else
                    LOG.severe("No root cause");

                if (cause != null) {
                    LOG.severe(String.format("Cause: %s", rcName));
                }
                else
                    LOG.severe("No cause");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                LOG.severe(sw.toString());
                LOG.severe("--- End of exception ---");

                FacesContext context = FacesContext.getCurrentInstance();
                ExternalContext extContext = context.getExternalContext();
                Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
                HttpServletRequest request = (HttpServletRequest) extContext.getRequest();
                HttpServletResponse response = (HttpServletResponse) extContext.getResponse();

                String errorMsg = "Unhandled Exception";
                String errorPage = "/error/error.xhtml";
                boolean isFatal = true;

                if (cause instanceof PermissionDenied) {
                    String message = String.format(
                            "Permission denied: \nPath: %s\nRemote IP: %s",
                            request.getRequestURL(),
                            request.getRemoteAddr());

                    LOG.warning(message);
                    errorMsg = "Access denied";
                    extContext.setResponseStatus(403);
                    isFatal = false;
                }
                else if (cause instanceof ELException) {
                    isFatal = true;
                }
                else if (cause instanceof ViewExpiredException) {
                    LOG.warning("View expired - invalidating session");
                    errorPage = "Login.xhtml";
                    isFatal = false;

                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        try {
                            session.invalidate();
                        }
                        catch (IllegalStateException ex) {
                            LOG.warning(String.format("Cannot invalidate session: %s", ex.getMessage()));
                            isFatal = true;
                        }
                    }
                    else {
                        LOG.warning("There's no session to invalidate");
                    }
                }
                else {
                    LOG.warning("##### Unknown server error");
                    extContext.setResponseStatus(500);
                }
                requestMap.put("errorMsg", errorMsg);

                if (!isFatal) {
                    try {
                        context.responseComplete();
                        LOG.info(String.format("Going to %s", errorPage));
                        try {
                            extContext.dispatch(errorPage);
                        }
                        catch (IOException ex) {
                            LOG.severe(String.format("Cannot go to error page: %s",
                            ex.getMessage()));
                        }
                        /*final ConfigurableNavigationHandler nav =
                                (ConfigurableNavigationHandler)
                                context.getApplication().getNavigationHandler();
                        nav.performNavigation(errPage);
                        context.renderResponse();*/
                    } catch (FacesException e) {
                        LOG.severe(String.format("Cannot dispatch error page: %s", e.getMessage()));
                    }
                    context.renderResponse();
                }
                else {
                    context.responseComplete();
                    LOG.info(String.format("Going to %s", errorPage));
                    try {
                        extContext.dispatch("/error/error.html");
                    }
                    catch (IOException ex) {
                        LOG.severe(String.format("Cannot go to error page: %s",
                        ex.getMessage()));
                    }
                    LOG.severe("#####  BEGIN ...---... #####");
                    if (rootCause != null)
                        LOG.severe(rootCause.toString());
                    else
                        LOG.severe("No root cause");
                    LOG.severe("#####  ...---... END #####");
                }
            } catch (Exception ex) {
                LOG.severe("An error occured while handling an error. :-((");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                LOG.severe(sw.toString());
            } finally {
                LOG.info("Removing exception from queue");
                queue.remove();
            }
        }
    }
}
