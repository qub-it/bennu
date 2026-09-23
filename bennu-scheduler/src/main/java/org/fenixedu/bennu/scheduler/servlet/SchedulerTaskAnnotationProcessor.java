package org.fenixedu.bennu.scheduler.servlet;

import java.util.Set;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.domain.SchedulerSystem;

@HandlesTypes({ Task.class })
public class SchedulerTaskAnnotationProcessor implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        if (classes != null) {
            for (Class<?> type : classes) {
                final Task annon = type.getAnnotation(Task.class);
                if (annon != null) {
                    SchedulerSystem.addTask(type.getName(), annon);
                }
            }
        }
    }
}
