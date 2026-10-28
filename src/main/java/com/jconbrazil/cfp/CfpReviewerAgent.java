package com.jconbrazil.cfp;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;

import java.util.logging.Logger;

// An agent is a CDI bean, and @WorkflowScoped gives every review request its own instance.
// @WorkflowScoped is already the default, but this app has no beans.xml: CDI only discovers
// classes with a bean-defining annotation, and @Agent is not one. Without the scope the agent
// is silently ignored.
@WorkflowScoped
@Agent(name = "cfp-reviewer", description = "Reviews talk proposals for the JCON Brazil call for papers.")
public class CfpReviewerAgent {

    private static final Logger LOGGER = Logger.getLogger(CfpReviewerAgent.class.getName());

    // The parameter type is the CDI event that starts the workflow:
    // every Event<Submission>.fire(...) runs a new workflow.
    @Trigger
    void onReviewRequested(Submission submission) {
        LOGGER.info(() -> "[TRIGGER] agent@" + Integer.toHexString(System.identityHashCode(this))
                + " received #" + submission.id() + " \"" + submission.talkTitle() + "\" by " + submission.speakerName());
    }
}
