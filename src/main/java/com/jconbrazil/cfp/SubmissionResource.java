package com.jconbrazil.cfp;

import jakarta.enterprise.event.Event;
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
    Event<Submission> reviewRequested;

    @GET
    public List<Submission> list() {
        return store.all();
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
        reviewRequested.fire(submission);   // synchronous: the workflow runs before we respond
        return Response.accepted(Map.of("message", "Review requested")).build();
    }
}
