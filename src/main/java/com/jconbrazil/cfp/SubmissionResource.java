package com.jconbrazil.cfp;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("submissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubmissionResource {

    @Inject
    SubmissionStore store;

    @Inject
    ReviewDispatcher dispatcher;

    @GET
    public List<Submission> list() {
        return store.all();
    }

    @GET
    @Path("{id}")
    public Submission get(@PathParam("id") long id) {
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public Response create(@Valid NewSubmission newSubmission) {
        Submission created = store.add(newSubmission);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("{id}/review")
    public Response review(@PathParam("id") long id) {
        Submission submission = store.find(id).orElseThrow(NotFoundException::new);
        dispatcher.dispatch(submission);   // the workflow runs on a managed thread
        return Response.accepted(Map.of("message", "Under review...")).build();
    }
}
