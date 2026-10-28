package com.jconbrazil.cfp;

import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// An agent is a plain Java class: its phases are tested by calling them directly,
// with the TCK's scripted LLM instead of a real model. No server, no Ollama.
class CfpReviewerAgentTest {

    private final LargeLanguageModelStub llm = new LargeLanguageModelStub();
    private final SubmissionStore store = new SubmissionStore();
    private final CfpReviewerAgent agent = new CfpReviewerAgent();

    private final Submission pitch = store.add(new NewSubmission("Fabio Vendas", "fabio@acmecloud.example",
            "AcmeCloud Enterprise Platform", "Book a demo with our sales team today!",
            Track.CLOUD_AND_DEVOPS, Level.BEGINNER, Format.TALK_45_MIN));

    @Test
    void salesPitchEndsTheWorkflowAndTellsTheSpeakerWhy() {
        llm.enqueueResponse("NO\nThis is an advertisement, not a talk.");

        Result result = agent.isTalkProposal(pitch, llm, store);

        assertFalse(result.success());
        String review = store.find(pitch.id()).orElseThrow().review();
        assertTrue(review.contains("This is an advertisement, not a talk."));
    }

    @Test
    void submissionIsSentToTheModelAsJson() {
        llm.enqueueResponse("YES\nIt is a talk.");

        agent.isTalkProposal(pitch, llm, store);

        assertTrue(llm.lastCall().effectivePrompt().contains("\"talkTitle\":\"AcmeCloud Enterprise Platform\""));
    }

    @Test
    void reviewIsReadEvenWhenTheModelWrapsItInMarkdown() {
        llm.enqueueResponse("""
                ```json
                {"score": 4, "strengths": ["clear"], "weaknesses": [], "comment": "Nice talk."}
                ```""");

        Review review = agent.review(llm);

        assertEquals(4, review.score());
        assertEquals("Nice talk.", review.comment());
    }

    @Test
    void replyThatIsNotAReviewIsAnLlmException() {
        llm.enqueueResponse("Sorry, I cannot review this talk.");

        assertThrows(LLMException.class, () -> agent.review(llm));
    }

    @Test
    void outcomePublishesAHumanReviewWhenNoReviewWasProduced() {
        agent.publish(pitch, null, store);

        assertEquals(Review.forHumanReview().comment(), store.find(pitch.id()).orElseThrow().review());
    }
}
