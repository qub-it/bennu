package org.fenixedu.bennu.io.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.io.domain.FileStorageConfiguration;
import org.fenixedu.bennu.io.domain.FileSupport;

import pt.ist.fenixframework.Atomic;

import com.google.gson.JsonElement;

@Path("/bennu-io/storage/config")
public class StorageConfigurationResource extends BennuRestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement all() {
        accessControl(Group.managers());
        createMissingConfigurations();
        return view(FileSupport.getInstance().getConfigurationSet(), "storageConfigurations");
    }

    @Atomic
    private void createMissingConfigurations() {
        FileStorageConfiguration.createMissingStorageConfigurations();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement post(JsonElement json) {
        accessControl(Group.managers());
        create(json, FileStorageConfiguration.class);
        return all();
    }

}
