"use strict";

// Browser feedback improves usability; server-side validation remains authoritative.
document.addEventListener("DOMContentLoaded", () => {
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
                const result = window.AccountPasswordStrength.evaluate(field.value);
                field.setCustomValidity(field.value && !result.valid
                    ? "Mật khẩu cần: " + result.problems.join(", ") + "." : "");
                if (meter) {
                    meter.dataset.valid = String(result.valid);
                    meter.dataset.level = result.level;
                    meter.querySelector("[data-strength-caption]").textContent = result.label;
                    const progress = meter.querySelector("[data-strength-progress]");
                    progress.setAttribute("aria-valuenow", String(result.percent));
                    progress.setAttribute("aria-valuetext", result.label);
                    meter.querySelector("[data-strength-fill]").style.width = result.percent + "%";
                    result.rules.forEach(rule => {
                        const item = meter.querySelector('[data-password-rule="' + rule.key + '"]');
                        if (item) {
                            item.dataset.met = String(rule.met);
                            item.setAttribute("aria-label", (rule.met ? "Đạt: " : "Chưa đạt: ") + item.textContent);
                        }
                    });
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


document.addEventListener("DOMContentLoaded", () => {
    // Confirm only destructive session actions, without changing the normal account forms.
    document.querySelectorAll("form[data-confirm]").forEach(form => {
        form.addEventListener("submit", event => {
            if (!window.confirm(form.dataset.confirm)) event.preventDefault();
        });
    });

    document.querySelectorAll("[data-lock-countdown]").forEach(element => {
        let seconds = Math.max(0, Number(element.dataset.seconds) || 0);
        const render = () => {
            if (seconds <= 0) {
                element.textContent = "Bạn có thể thử đăng nhập lại.";
                return;
            }
            const minutes = Math.floor(seconds / 60);
            const remainder = String(seconds % 60).padStart(2, "0");
            element.textContent = "Thử lại sau " + minutes + ":" + remainder + ".";
        };
        render();
        const timer = window.setInterval(() => {
            seconds = Math.max(0, seconds - 1);
            render();
            if (seconds === 0) window.clearInterval(timer);
        }, 1000);
    });
});
