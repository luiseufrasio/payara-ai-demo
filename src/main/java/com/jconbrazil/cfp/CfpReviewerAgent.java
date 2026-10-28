package com.jconbrazil.cfp;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;

import java.util.logging.Logger;
import java.util.stream.Collectors;

// An agent is a CDI bean, and @WorkflowScoped gives every review request its own instance.
// @WorkflowScoped is already the default, but this app has no beans.xml: CDI only discovers
// classes with a bean-defining annotation, and @Agent is not one. Without the scope the agent
// is silently ignored.
@WorkflowScoped
@Agent(name = "cfp-reviewer", description = "Reviews talk proposals for the JCON Brazil call for papers.")
public class CfpReviewerAgent {

    private static final Logger LOGGER = Logger.getLogger(CfpReviewerAgent.class.getName());

    private static final String PROPOSAL_PROMPT = """
            You screen the call for papers of JCON Brazil, a conference for Java developers.
            A talk proposal describes concrete technical content for Java developers.
            Sales pitches, advertisements and one-line abstracts are not talk proposals.
            Is the following submission a real talk proposal?
            Answer YES or NO on the first line, then give the reason in one sentence on the second line.
            Submission: {}
            """;

    // The parameter type is the CDI event that starts the workflow:
    // every Event<Submission>.fire(...) runs a new workflow.
    @Trigger
    void onReviewRequested(Submission submission) {
        LOGGER.info(() -> "[TRIGGER] agent@" + Integer.toHexString(System.identityHashCode(this))
                + " received #" + submission.id() + " \"" + submission.talkTitle() + "\" by " + submission.speakerName());
    }

    // Is it a real talk proposal? Result(false, ...) ends the workflow here.
    // The Submission comes from the workflow state, the LargeLanguageModel from the runtime.
    // JSON Binding serializes the submission into the {} placeholder.
    @Decision
    Result isTalkProposal(Submission submission, LargeLanguageModel llm) {
        String answer = llm.query(PROPOSAL_PROMPT, submission).strip();
        String verdict = answer.lines().findFirst().orElse("");
        String reason = answer.lines().skip(1).collect(Collectors.joining(" ")).strip();

        boolean proposal = verdict.toUpperCase().contains("YES");
        LOGGER.info(() -> "[DECISION] talk proposal: " + proposal + " - " + reason);
        return new Result(proposal, reason);
    }
}
