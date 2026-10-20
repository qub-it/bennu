package org.fenixedu.bennu.scheduler.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.scheduler.api.json.TaskScheduleJsonAdapter;
import org.fenixedu.bennu.scheduler.domain.SchedulerSystem;
import org.fenixedu.bennu.scheduler.domain.TaskSchedule;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Path("/bennu-scheduler/schedule")
public class ScheduleResource extends BennuRestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement getSchedule() {
        accessControl(Group.managers());
        return view(SchedulerSystem.getInstance().getTaskScheduleSet(), "schedule");
    }

    @GET
    @Path("dump")
    public Response dump() {
        accessControl(Group.managers());
        final String filename =
                ScheduleResource.class.getSimpleName() + "_" + new DateTime().toString("MM-dd-yyyy-kk-mm-ss") + ".json";

        return Response.ok(view(SchedulerSystem.getInstance().getTaskScheduleSet(), "schedule"))
                .header("Content-Disposition", "attachment; filename=" + filename).build();
    }

    @DELETE
    public Response delete() {
        accessControl(Group.managers());
        clearAllSchedules();
        return ok();
    }

    @POST
    @Deprecated
    @Path("dump")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response loadDump(@FormParam("data") JsonObject json) {
        accessControl(Group.managers());
        clearAllSchedules();
        createSchedulesFromDump(json);
        return ok();
    }

    @POST
    @Path("load-dump")
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonElement loadDumpNew(JsonObject json) {
        accessControl(Group.managers());
        clearAllSchedules();
        createSchedulesFromDump(json);
        return getSchedule();
    }

    @Atomic(mode = TxMode.WRITE)
    public void createSchedulesFromDump(JsonObject json) {
        TaskScheduleJsonAdapter taskScheduleJsonAdapter = new TaskScheduleJsonAdapter();
        for (JsonElement schedule : json.get("schedule").getAsJsonArray()) {
            taskScheduleJsonAdapter.create(schedule, null);
        }
    }

    @Atomic(mode = TxMode.WRITE)
    public void clearAllSchedules() {
        for (TaskSchedule schedule : SchedulerSystem.getInstance().getTaskScheduleSet()) {
            schedule.delete();
        }
    }

    @GET
    @Path("{oid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement get(@PathParam("oid") String taskOid) {
        accessControl(Group.managers());
        return view(readDomainObject(taskOid));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement addSchedule(JsonElement configJson) {
        accessControl(Group.managers());
        return view(create(configJson, TaskSchedule.class));
    }

    @PUT
    @Path("{oid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement changeSchedule(JsonElement taskScheduleJson, @PathParam("oid") String taskScheduleOid) {
        accessControl(Group.managers());
        return view(update(taskScheduleJson, readDomainObject(taskScheduleOid)));
    }

    @DELETE
    @Path("{oid}")
    public Response delete(@PathParam("oid") String taskOid) {
        accessControl(Group.managers());
        TaskSchedule schedule = readDomainObject(taskOid);
        schedule.delete();
        return ok();
    }

}
