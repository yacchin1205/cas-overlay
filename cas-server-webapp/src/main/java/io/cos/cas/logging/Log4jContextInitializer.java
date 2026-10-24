package io.cos.cas.logging;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.logging.log4j.web.Log4jServletContextListener;
import org.apache.logging.log4j.web.Log4jWebSupport;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;

/**
 * Uses the initialization listener explicitly because Log4j also provides a shutdown-only listener.
 * @author Codex
 * @since 4.1.5
 */
public final class Log4jContextInitializer implements ServletContextAware, DisposableBean {

    private final Log4jServletContextListener listener = new Log4jServletContextListener();

    private final String configurationLocation;

    private ServletContext servletContext;

    /**
     * Resolves the external logging configuration before initialization.
     * @param logConfigurationFile the configured Log4j resource
     * @throws IOException if the resource URI cannot be resolved
     */
    public Log4jContextInitializer(final Resource logConfigurationFile) throws IOException {
        this.configurationLocation = logConfigurationFile.getURI().toString();
    }

    @Override
    public void setServletContext(final ServletContext context) {
        this.servletContext = context;
        context.setInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION, this.configurationLocation);
        this.listener.contextInitialized(new ServletContextEvent(context));
    }

    @Override
    public void destroy() {
        this.listener.contextDestroyed(new ServletContextEvent(this.servletContext));
    }
}
