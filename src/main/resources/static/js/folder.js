const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

async function sendJson(url, method, body) {
    const headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest"
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    const response = await fetch(url, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Запрос не выполнен");
    }
    return response.json();
}

function formValue(form, name) {
    return new FormData(form).get(name);
}

document.querySelectorAll("[data-ajax-rename-folder]").forEach((form) => {
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            await sendJson(form.action, "PATCH", {name: formValue(form, "name")});
            window.location.reload();
        } catch (error) {
            alert(error.message);
        }
    });
});

document.querySelectorAll("[data-ajax-delete-folder]").forEach((form) => {
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!confirm(form.dataset.confirmMessage || "Удалить папку?")) {
            return;
        }
        try {
            await sendJson(form.action, "DELETE");
            window.location.href = form.dataset.redirect || "/app";
        } catch (error) {
            alert(error.message);
        }
    });
});

document.querySelectorAll("[data-ajax-share]").forEach((form) => {
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            await sendJson(form.action, "POST", {
                email: formValue(form, "email"),
                accessLevel: formValue(form, "accessLevel")
            });
            form.reset();
            alert("Доступ выдан");
        } catch (error) {
            alert(error.message);
        }
    });
});

const fileInfoModal = document.querySelector("[data-file-info-modal]");
const fileInfoClose = document.querySelector("[data-file-info-close]");
const fileInfoFields = fileInfoModal
    ? Object.fromEntries(
        Array.from(fileInfoModal.querySelectorAll("[data-file-info-field]"))
            .map((field) => [field.dataset.fileInfoField, field])
    )
    : {};

function setFileInfoField(name, value) {
    if (fileInfoFields[name]) {
        fileInfoFields[name].textContent = value || "—";
    }
}

function closeFileInfoModal() {
    if (fileInfoModal) {
        fileInfoModal.hidden = true;
        document.body.classList.remove("modal-open");
    }
}

document.querySelectorAll("[data-file-info-button]").forEach((button) => {
    button.addEventListener("click", () => {
        if (!fileInfoModal) {
            return;
        }

        setFileInfoField("name", button.dataset.name);
        setFileInfoField("originalName", button.dataset.originalName);
        setFileInfoField("contentType", button.dataset.contentType);
        setFileInfoField("extension", button.dataset.extension);
        setFileInfoField("size", button.dataset.size);
        setFileInfoField("createdAt", button.dataset.createdAt);
        setFileInfoField("updatedAt", button.dataset.updatedAt);
        setFileInfoField("scanStatus", button.dataset.scanStatus);
        setFileInfoField("scanPassed", button.dataset.scanPassed);
        setFileInfoField("scanMessage", button.dataset.scanMessage);
        setFileInfoField("scanThreats", button.dataset.scanThreats);

        fileInfoModal.hidden = false;
        document.body.classList.add("modal-open");
        fileInfoClose?.focus();
    });
});

fileInfoClose?.addEventListener("click", closeFileInfoModal);

fileInfoModal?.addEventListener("click", (event) => {
    if (event.target === fileInfoModal) {
        closeFileInfoModal();
    }
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        closeFileInfoModal();
    }
});
