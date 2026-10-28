package com.jconbrazil.cfp;

import java.util.List;

// The program committee's review of a submission, as produced by the LLM.
// score: 1 (reject) to 5 (must accept)
public record Review(int score, List<String> strengths, List<String> weaknesses, String comment) {

    // What the "Review" column of the page shows
    public String asText() {
        return score + "/5 - " + comment;
    }
}
