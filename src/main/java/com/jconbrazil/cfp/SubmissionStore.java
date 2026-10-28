package com.jconbrazil.cfp;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class SubmissionStore {

    private final List<Submission> submissions = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public SubmissionStore() {
        add(new NewSubmission("Ana Souza", "ana@example.com",
                "Virtual Threads in Production: What We Learned",
                "We moved a payment gateway with 40k requests per second to virtual threads. "
                        + "This talk shows the pinning problems we hit, how we found them with JFR, "
                        + "and the before/after numbers. You leave with a checklist for your own migration.",
                Track.CORE_JAVA, Level.INTERMEDIATE, Format.TALK_45_MIN));
        add(new NewSubmission("Bruno Lima", "bruno@example.com",
                "Jakarta Data from Zero to Repository",
                "A hands-on workshop: we start with an empty Jakarta EE 11 project and build "
                        + "repositories with Jakarta Data, including pagination and custom queries.",
                Track.JAKARTA_EE, Level.BEGINNER, Format.WORKSHOP_2H));
        add(new NewSubmission("Carla Mendes", "carla@example.com",
                "Five Minutes of Ollama, Ten Minutes of Java",
                "A quick tour of calling a local LLM from plain Java with java.net.http, no frameworks.",
                Track.AI_AND_JAVA, Level.BEGINNER, Format.LIGHTNING_15_MIN));
    }

    public List<Submission> all() {
        return List.copyOf(submissions);
    }

    public Optional<Submission> find(long id) {
        return submissions.stream().filter(s -> s.id() == id).findFirst();
    }

    public Submission add(NewSubmission in) {
        Submission submission = new Submission(
                nextId.getAndIncrement(),
                in.speakerName(),
                in.email(),
                in.talkTitle(),
                in.abstractText(),
                in.track(),
                in.level(),
                in.format(),
                Instant.now(),
                null);
        submissions.add(submission);
        return submission;
    }
}
