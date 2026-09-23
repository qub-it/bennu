package org.fenixedu.bennu.portal.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.api.json.AuthenticatedUserViewer;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.portal.api.json.PortalMenuViewer;
import org.fenixedu.bennu.portal.domain.PortalConfiguration;

import com.google.gson.JsonObject;

@Path("/bennu-portal/data")
public class PortalDataResource extends BennuRestResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getMenu() {
        final JsonObject hostMenuView = new JsonObject();
        merge(hostMenuView, getBuilder().view(PortalConfiguration.getInstance(), PortalMenuViewer.class).getAsJsonObject());
        merge(hostMenuView, getBuilder().view(null, Void.class, AuthenticatedUserViewer.class).getAsJsonObject());
        return hostMenuView;
    }
}
