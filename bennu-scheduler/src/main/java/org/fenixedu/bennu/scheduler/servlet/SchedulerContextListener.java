package org.fenixedu.bennu.scheduler.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.fenixedu.bennu.scheduler.domain.SchedulerSystem;

@WebListener
public class SchedulerContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        SchedulerSystem.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        SchedulerSystem.destroy();
    }
}
