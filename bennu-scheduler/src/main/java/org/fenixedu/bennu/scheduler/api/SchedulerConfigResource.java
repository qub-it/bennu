package org.fenixedu.bennu.scheduler.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.io.domain.LocalFileSystemStorage;
import org.fenixedu.bennu.scheduler.domain.SchedulerSystem;
import org.fenixedu.bennu.scheduler.log.ExecutionLogRepository;
import org.fenixedu.bennu.scheduler.log.FileSystemLogRepository;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

import com.google.gson.JsonElement;

@Path("/bennu-scheduler/config")
public class SchedulerConfigResource extends BennuRestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement getLoggingStorage() {
        accessControl(Group.managers());
        return view(SchedulerSystem.getInstance());
    }

    @PUT
    @Path("/{oid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement changeLoggingStorage(@PathParam("oid") String loggingStorageExternalId) {
        accessControl(Group.managers());
        innerSetLoggingStorage(loggingStorageExternalId);
        return getLoggingStorage();
    }

    @Atomic(mode = TxMode.WRITE)
    public void innerSetLoggingStorage(String loggingStorageExternalId) {
        LocalFileSystemStorage storage = readDomainObject(loggingStorageExternalId);
        SchedulerSystem.getInstance().setLoggingStorage(storage);
        ExecutionLogRepository repository = SchedulerSystem.getInstance().getLogRepository();
        if (repository instanceof FileSystemLogRepository) {
            ((FileSystemLogRepository) repository).setBasePath(SchedulerSystem.getInstance().getLogsPath());
        }
    }

}
