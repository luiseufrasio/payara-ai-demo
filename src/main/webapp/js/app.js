const form = document.getElementById("submission-form");
const formMessage = document.getElementById("form-message");
const tableBody = document.getElementById("submissions");

// Shows "Core Java" instead of "CORE_JAVA", using the texts of the form's <option>s.
function label(selectId, value) {
    const option = document.querySelector(`#${selectId} option[value="${value}"]`);
    return option ? option.textContent : value;
}

function showMessage(element, text, isError) {
    element.textContent = text;
    element.classList.toggle("error", isError);
}

async function loadSubmissions() {
    const response = await fetch("api/submissions");
    const submissions = await response.json();
    tableBody.replaceChildren(...submissions.map(toRow));
}

function toRow(submission) {
    const row = document.createElement("tr");
    row.innerHTML = `
        <td>${submission.id}</td>
        <td><strong></strong><br><span class="speaker"></span></td>
        <td>${label("track", submission.track)}<br>
            ${label("level", submission.level)} · ${label("format", submission.format)}</td>
        <td>
            <button type="button" class="btn-review">Request review</button>
            <p class="message"></p>
        </td>`;
    // textContent, so user input is never interpreted as HTML
    row.querySelector("strong").textContent = submission.talkTitle;
    row.querySelector(".speaker").textContent = submission.speakerName;
    const reviewMessage = row.querySelector(".message");
    reviewMessage.textContent = submission.review ?? "";
    row.querySelector("button").addEventListener("click", () => requestReview(submission.id, reviewMessage));
    return row;
}

async function requestReview(id, messageElement) {
    showMessage(messageElement, "Requesting review…", false);
    const response = await fetch(`api/submissions/${id}/review`, { method: "POST" });
    const body = await response.json();
    showMessage(messageElement, body.review ?? body.message ?? JSON.stringify(body), !response.ok);
    if (response.status === 202) {
        pollReview(id, messageElement);
    }
}

// The agent runs in the background: ask every 2 seconds until the review is there.
async function pollReview(id, messageElement) {
    await new Promise(resolve => setTimeout(resolve, 2000));
    const response = await fetch(`api/submissions/${id}`);
    const submission = await response.json();
    if (submission.review) {
        showMessage(messageElement, submission.review, false);
    } else {
        pollReview(id, messageElement);
    }
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(form));
    const response = await fetch("api/submissions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });
    const body = await response.json();

    if (response.ok) {
        showMessage(formMessage, `Thanks! Your talk was submitted as #${body.id}.`, false);
        form.reset();
        loadSubmissions();
    } else {
        showMessage(formMessage, [body.message, ...(body.errors ?? [])].join(" — "), true);
    }
});

loadSubmissions();
