package org.fenixedu.bennu.portal.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.portal.domain.PortalConfiguration;

import com.google.gson.JsonElement;

@Path("/bennu-portal/configuration")
public class PortalConfigurationResource extends BennuRestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement viewConfig() {
        accessControl(Group.managers());
        return view(PortalConfiguration.getInstance());
    }

    @GET
    @Path("logo")
    public Response logo(@HeaderParam("If-None-Match") String ifNoneMatch) {
        final PortalConfiguration instance = PortalConfiguration.getInstance();
        if (instance != null && instance.getLogo() != null) {
            EntityTag etag = buildETag(instance);
            if (etag.toString().equals(ifNoneMatch)) {
                return Response.notModified(etag).build();
            }
            return Response.ok(instance.getLogo(), instance.getLogoType())
                    .header(HttpHeaders.CACHE_CONTROL, CoreConfiguration.getConfiguration().staticCacheControl()).tag(etag)
                    .build();
        }
        throw new WebApplicationException(Status.NOT_FOUND);
    }

    private EntityTag buildETag(PortalConfiguration instance) {
        return EntityTag.valueOf("W/\"" + instance.getLogo().length + "-" + instance.getExternalId() + "\"");
    }

    @GET
    @Path("favicon")
    public Response favicon() {
        final PortalConfiguration instance = PortalConfiguration.getInstance();
        if (instance != null && instance.getFavicon() != null) {
            return Response.ok(instance.getFavicon(), instance.getFaviconType())
                    .header(HttpHeaders.CACHE_CONTROL, CoreConfiguration.getConfiguration().staticCacheControl()).build();
        }
        throw new WebApplicationException(Status.NOT_FOUND);
    }

    @PUT
    @Path("{oid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement updateConfig(JsonElement jsonData, @PathParam("oid") String oid) {
        accessControl(Group.managers());
        return view(update(jsonData, readDomainObject(oid)));
    }

}
