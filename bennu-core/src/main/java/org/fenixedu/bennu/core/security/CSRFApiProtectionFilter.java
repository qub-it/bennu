package org.fenixedu.bennu.core.security;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.common.base.Strings;

/***
 * 
 * Prematching filter to prevent CSRF attacks
 * 
 * It will check if the header "X-Requested-With" is present in the request, and sends a {@link Status#BAD_REQUEST} response if
 * not.
 * 
 * @author Sérgio Silva (sergio.silva@tecnico.ulisboa.pt)
 * 
 * @see CSRFFeature
 *
 */

@PreMatching
public class CSRFApiProtectionFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        if (Strings.isNullOrEmpty(ctx.getHeaderString("X-Requested-With"))) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST)
                            .entity("To make a successful request to this endpoint you must send X-Requested-With header with a non empty string value.")
                            .location(ctx.getUriInfo().getBaseUri()).build());
        }
    }
}
