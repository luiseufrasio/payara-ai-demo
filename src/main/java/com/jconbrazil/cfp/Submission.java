package com.jconbrazil.cfp;

import jakarta.json.bind.annotation.JsonbProperty;

import java.time.Instant;

// "abstract" is a Java keyword, so the component is abstractText and the JSON field is "abstract".
public record Submission(
        long id,
        String speakerName,
        String email,
        String talkTitle,
        @JsonbProperty("abstract") String abstractText,
        Track track,
        Level level,
        Format format,
        Instant submittedAt,
        String review) {

    public Submission withReview(String review) {
        return new Submission(id, speakerName, email, talkTitle, abstractText, track, level, format, submittedAt, review);
    }
}
