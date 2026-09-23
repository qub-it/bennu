package org.fenixedu.bennu.scheduler.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.scheduler.custom.ClassBean;

import com.google.gson.JsonObject;

@Path("/bennu-scheduler/custom")
public class CustomTaskResource extends BennuRestResource {

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCustomTask(JsonObject jsonCode) {
        accessControl(Group.managers());
        getClassBean(jsonCode).run();
        return ok();
    }

    private ClassBean getClassBean(JsonObject jsonCode) {
        return new ClassBean(jsonCode.get("name").getAsString(), jsonCode.get("code").getAsString());
    }

    @PUT
    @Path("compile")
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonObject compileCustomTask(JsonObject jsonCode) {
        accessControl(Group.managers());
        return getClassBean(jsonCode).compile();
    }

}