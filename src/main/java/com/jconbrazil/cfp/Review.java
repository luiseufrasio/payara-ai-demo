package com.jconbrazil.cfp;

import java.util.List;

// The program committee's review of a submission, as produced by the LLM.
// score: 1 (reject) to 5 (must accept)
public record Review(int score, List<String> strengths, List<String> weaknesses, String comment) {

    // Published when the submission is not a talk proposal, so the speaker learns why
    public static Review notAProposal(String reason) {
        return new Review(1, List.of(), List.of(reason),
                "Thank you for submitting. This is not a talk proposal yet: " + reason);
    }

    // Published when the automatic review failed and a person has to step in
    public static Review forHumanReview() {
        return new Review(0, List.of(), List.of(),
                "The automatic review is unavailable. A member of the program committee will review this submission.");
    }

    // What the "Review" column of the page shows
    public String asText() {
        return score > 0 ? score + "/5 - " + comment : comment;
    }
}
