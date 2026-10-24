package io.cos.cas.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.web.Log4jWebSupport;
import org.apache.logging.log4j.web.WebLoggerContextUtils;
import org.junit.Test;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Verifies the Spring-managed Log4j lifecycle.
 * @since 4.1.5
 */
public class Log4jContextInitializerTests {

    @Test
    public void initializesConfiguredLoggerAndStopsWithSpringContext() {
        final MockServletContext servletContext = new MockServletContext();
        servletContext.setInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED, "true");

        final Properties properties = new Properties();
        properties.setProperty("log4j.config.location", "classpath:log4j-lifecycle-test.xml");
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(properties);

        final LoggerContext loggerContext;
        try (XmlWebApplicationContext context = new XmlWebApplicationContext()) {
            context.setServletContext(servletContext);
            context.setConfigLocation("file:src/main/webapp/WEB-INF/spring-configuration/log4jConfiguration.xml");
            context.addBeanFactoryPostProcessor(configurer);
            context.refresh();

            loggerContext = WebLoggerContextUtils.getRequiredWebLoggerContext(servletContext);
            assertTrue(loggerContext.isStarted());
            assertEquals("cas-log4j-lifecycle-test", loggerContext.getConfiguration().getName());
        }
        assertTrue(loggerContext.isStopped());
    }
}
