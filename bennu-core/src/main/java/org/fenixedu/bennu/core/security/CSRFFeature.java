package org.fenixedu.bennu.core.security;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/***
 *
 * Apply {@link CSRFApiProtectionFilter} if resourceMethod is annotated with {@link POST}, {@link PUT} or {@link DELETE}
 *
 * @author Sérgio Silva (sergio.silva@tecnico.ulisboa.pt)
 *
 */
@Provider
public class CSRFFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(CSRFFeature.class);

    private static final Set<Class<? extends Annotation>> toFilterAnnotations = ImmutableSet.of();

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        if (Stream.of(resourceInfo.getResourceMethod().getAnnotations()).map(Annotation::annotationType).anyMatch(
                toFilterAnnotations::contains) && !resourceInfo.getResourceMethod().isAnnotationPresent(SkipCSRF.class)) {
            logger.debug("Enabling CSRF protection for {}", resourceInfo.getResourceMethod());
            context.register(new CSRFApiProtectionFilter());
        }
    }

}
