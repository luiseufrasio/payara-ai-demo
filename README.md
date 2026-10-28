# JCON Brazil 2030 – Call for Papers

A small CFP (call for papers) app: speakers submit talk proposals on a web
page, and the proposals show up in a table.

> **JCON Brazil is a fictional event for a live-coding demo.**
> This project is the starting point for a live session in which we add an AI
> reviewer agent with Jakarta Agentic AI.

## Watch the live session

We fill this repository live on stream:
**[youtube.com/live/bm4BRjW0pUs](https://youtube.com/live/bm4BRjW0pUs)**

<a href="https://youtube.com/live/bm4BRjW0pUs">
  <img src="https://img.youtube.com/vi/bm4BRjW0pUs/mqdefault.jpg" width="320" alt="Live coding session: Jakarta Agentic AI on Payara">
</a>

## Run it

You only need Docker:

```bash
docker compose up --build
```

This starts two containers:

| Container | What it does |
|-----------|--------------|
| `payara`  | Builds the WAR with Maven and deploys it on the stock `payara/micro:7.2026.9` image |
| `ollama`  | Runs Ollama and pulls `gemma3:4b` on first start (several GB, cached in a volume) |

The web app does not wait for the model download. `docker compose ps` shows
`ollama` as *healthy* once `gemma3:4b` is available.

## URLs

| URL | What |
|-----|------|
| http://localhost:8080/ | The CFP page |
| http://localhost:8080/api/submissions | All submissions as JSON |
| http://localhost:11434/api/tags | Models available in Ollama |

## API

| Method | Path | |
|--------|------|-|
| `GET`  | `/api/submissions` | List all submissions (3 are seeded) |
| `POST` | `/api/submissions` | Create a submission; returns 400 with the validation errors |
| `GET`  | `/api/submissions/{id}` | One submission, with its `review` once the agent is done |
| `POST` | `/api/submissions/{id}/review` | Starts the reviewer agent in the background; returns 202 |

```bash
curl -X POST http://localhost:8080/api/submissions/1/review
```

## Stack

- Java 21, Maven, WAR
- Jakarta EE 11 Web Profile (JAX-RS, CDI, Bean Validation, JSON-B)
- `jakarta.agentic-ai:jakarta.agentic-ai-api:1.0.0-M1` (provided, not used yet)
- Payara Micro `7.2026.9` (bundles the Agentic AI implementation)
- Ollama with `gemma3:4b`
- Plain HTML, CSS and JavaScript. No frameworks, no database: everything is in memory.

## Project layout

```
src/main/java/com/jconbrazil/cfp/
  Submission.java            the record that is stored and returned
  NewSubmission.java         what the form sends, with validation
  Track / Level / Format     enums
  SubmissionStore.java       @ApplicationScoped in-memory store, 3 seeded talks
  SubmissionResource.java    REST endpoints under /api/submissions
  ValidationErrorMapper.java validation errors as JSON
  RestApplication.java       @ApplicationPath("/api")
src/main/resources/META-INF/microprofile-config.properties   LLM settings
src/main/webapp/             index.html, css/style.css, js/app.js, img/
samples/                     abstracts to paste during the stream
```

The logo goes to `src/main/webapp/img/jcon-brazil-2030.png`. Without it the
page shows the text "JCON Brazil 2030" instead.

## LLM configuration

Payara reads the LLM settings with MicroProfile Config
(`src/main/resources/META-INF/microprofile-config.properties`):

```properties
payara.agentic.llm.provider=ollama
payara.agentic.llm.model=gemma3:4b
payara.agentic.llm.ollama.base-url=http://localhost:11434
```

In Docker Compose, the environment variable
`PAYARA_AGENTIC_LLM_OLLAMA_BASE_URL=http://ollama:11434` overrides the base URL.
The key names come from `LlmBackendFactory` in the Payara source
(`appserver/agentic-ai/agentic-ai-core`, tag `payara-server-7.2026.9`).

## Live session

The seam is `SubmissionResource.review(...)`:

```java
@POST
@Path("{id}/review")
public Response review(@PathParam("id") long id) {
    // TODO live: fire the agent trigger here
    return Response.status(501)
            .entity(Map.of("message", "Reviewer agent not built yet"))
            .build();
}
```

Today the "Request review" button on the page shows
*Reviewer agent not built yet*. In the live session we build a reviewer agent
with Jakarta Agentic AI, trigger it from this method, and store its result in
the `review` field of the `Submission`. The page already shows that field in
the "Review" column.

To try the agent, paste the abstracts from `samples/`:

1. `01-strong-talk.md` – a good, concrete proposal
2. `02-needs-live-demo.md` – only works with a live demo on stage
3. `03-not-an-abstract.md` – a sales pitch / a single line

## The reviewer agent

`CfpReviewerAgent` is built live, one branch per step:

| Branch | Step |
|--------|------|
| `0-initial` | The starter app: "Request review" answers 501 |
| `1-trigger` | `@Agent` + `@Trigger`: a review request is a CDI event |
| `2-decision` | `@Decision`: the LLM checks that it is a real talk proposal |
| `3-action` | `@Action`: the LLM writes a structured `Review` |
| `4-outcome` | `@Outcome`: the review is published on the submission |
| `5-exception` | `@HandleException`: when the LLM fails, a human reviews |
| `6-plain-java` | What still ends up in plain Java |

The agent is annotated `@WorkflowScoped` although that is the default scope:
this app has no `beans.xml`, so CDI only discovers classes with a
bean-defining annotation, and `@Agent` is not one. Without a scope the agent
is ignored, with no error.

### What still ends up in plain Java

| Need | Where |
|------|-------|
| Tell the speaker why a submission was rejected: a negative `Result` ends the workflow and no later phase sees it | The `@Decision` writes the rejection itself |
| Read JSON from a local model that wraps it in markdown fences | `jsonIn(...)` + JSON Binding in the `@Action` |
| A fallback review after an LLM failure: handlers return `void` | The `@Outcome` accepts a missing `Review` |
| Do not block the HTTP request: the trigger is observed synchronously, so `Event.fireAsync` never reaches the agent | `ReviewDispatcher` fires on a `ManagedExecutorService`; the page polls |
| Unit tests | `CfpReviewerAgentTest` calls the phases directly with the TCK's `LargeLanguageModelStub` |

## Run without Docker

With Ollama on your machine:

```bash
mvn package
java -jar payara-micro-7.2026.9.jar --deploy target/ROOT.war
```

Override the model or the Ollama URL with system properties, for example
`-Dpayara.agentic.llm.model=gemma3:12b`.

## Build and test without Docker

```bash
mvn package
```

Runs the tests for the REST resource and the agent (no server, no Ollama) and
produces `target/ROOT.war`.
