"use strict";

// Browser feedback improves usability; server-side validation remains authoritative.
document.addEventListener("DOMContentLoaded", () => {
    const utf8Length = value => new TextEncoder().encode(value).length;

    function passwordProblems(value) {
        const problems = [];
        if (value.length < 8 || value.length > 64) problems.push("8–64 ký tự");
        if (!/\p{Lu}/u.test(value)) problems.push("chữ hoa");
        if (!/\p{Ll}/u.test(value)) problems.push("chữ thường");
        if (!/\p{Nd}/u.test(value)) problems.push("chữ số");
        if (!/[^\p{L}\p{N}\s]/u.test(value)) problems.push("ký tự đặc biệt");
        if (/\s/u.test(value)) problems.push("không có khoảng trắng");
        if (utf8Length(value) > 72) problems.push("tối đa 72 byte UTF-8");
        return problems;
    }

    document.querySelectorAll("[data-toggle-password]").forEach(button => {
        button.addEventListener("click", () => {
            const field = document.getElementById(button.dataset.togglePassword);
            const show = field.type === "password";
            field.type = show ? "text" : "password";
            button.textContent = show ? "Ẩn" : "Hiện";
            button.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
            button.setAttribute("aria-pressed", String(show));
        });
    });

    document.querySelectorAll("form[data-validate]").forEach(form => {
        // Let native validation run after assigning cross-field validation messages.
        form.noValidate = true;
        const strongFields = form.querySelectorAll("[data-strong-password]");
        const matchFields = form.querySelectorAll("[data-match]");
        const meter = form.querySelector("[data-password-meter]");

        function validatePasswords() {
            strongFields.forEach(field => {
                const problems = passwordProblems(field.value);
                field.setCustomValidity(field.value && problems.length
                    ? "Mật khẩu cần: " + problems.join(", ") + "." : "");
                if (meter) {
                    meter.dataset.valid = String(field.value.length > 0 && problems.length === 0);
                    meter.textContent = !field.value ? "" : problems.length
                        ? "Cần thêm: " + problems.join(", ") : "Mật khẩu đáp ứng yêu cầu.";
                }
            });
            matchFields.forEach(field => {
                const source = document.getElementById(field.dataset.match);
                field.setCustomValidity(field.value && source && field.value !== source.value
                    ? "Mật khẩu xác nhận không khớp." : "");
            });
        }

        form.addEventListener("input", event => {
            if (event.target instanceof HTMLInputElement) {
                event.target.classList.remove("is-invalid");
                event.target.removeAttribute("aria-invalid");
                validatePasswords();
            }
        });
        form.addEventListener("submit", event => {
            validatePasswords();
            if (!form.checkValidity()) {
                event.preventDefault();
                form.classList.add("was-validated");
                form.reportValidity();
                const invalid = form.querySelector(":invalid");
                if (invalid) invalid.focus();
            } else {
                const button = form.querySelector('button[type="submit"]');
                if (button) {
                    button.disabled = true;
                    button.textContent = "Đang xử lý…";
                }
            }
        });
        form.querySelectorAll(".is-invalid").forEach(field => {
            field.setAttribute("aria-invalid", "true");
        });
    });
    const firstError = document.querySelector("input.is-invalid");
    if (firstError) firstError.focus();
});
