package org.fenixedu.bennu.io.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.io.domain.DomainStorage;
import org.fenixedu.bennu.io.domain.FileStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.bennu.io.domain.LocalFileSystemStorage;

import pt.ist.fenixframework.Atomic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Path("/bennu-io/storage")
public class FileStorageResource extends BennuRestResource {

    @POST
    @Path("/default/{storage}")
    public JsonElement setDefault(@PathParam("storage") String storageId) {
        accessControl(Group.managers());
        innerSetDefault(this.<FileStorage> readDomainObject(storageId));
        return all();
    }

    @Atomic
    private void innerSetDefault(FileStorage storage) {
        FileSupport.getInstance().setDefaultStorage(storage);
    }

    @POST
    @Path("/domain/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement createDomainStorage(@PathParam("name") String name) {
        accessControl(Group.managers());
        return view(createDomainStorageService(name));
    }

    @POST
    @Path("/lfs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement createLFSStorage(JsonElement json) {
        accessControl(Group.managers());
        return view(create(json, LocalFileSystemStorage.class));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement all() {
        accessControl(Group.managers());
        return view(FileSupport.getInstance().getFileStorageSet(), "storages");
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject fileCount() {
        accessControl(Group.managers());
        JsonObject json = new JsonObject();
        for (FileStorage store : FileSupport.getInstance().getFileStorageSet()) {
            json.addProperty(store.getExternalId(), store.getFileSet().size());
        }
        return json;
    }

    @Atomic
    private DomainStorage createDomainStorageService(String name) {
        return FileStorage.createNewDomainStorage(name);
    }

    @DELETE
    @Path("{oid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement delete(@PathParam("oid") String storageOid) {
        accessControl(Group.managers());
        final FileStorage fileStorage = (FileStorage) readDomainObject(storageOid);
        final JsonElement response = view(fileStorage);
        Boolean deleteStorage = deleteStorage(fileStorage);
        if (deleteStorage) {
            return response;
        }
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Atomic
    private Boolean deleteStorage(FileStorage fileStorage) {
        return fileStorage.delete();
    }

    @PUT
    @Path("/convert/{oid}")
    public Response convertFileStorage(@PathParam("oid") String fileStorageOid) {
        accessControl(Group.managers());
        GenericFile.convertFileStorages((FileStorage) readDomainObject(fileStorageOid));
        return ok();
    }

}
