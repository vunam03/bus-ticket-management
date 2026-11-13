"use strict";
// Prevent accidental double clicks without removing the clicked button's name/value.
document.querySelectorAll("form[data-submit-once]").forEach(form => {
    form.addEventListener("submit", event => {
        if (form.dataset.busy === "true") {
            event.preventDefault();
            return;
        }
        const question = form.dataset.confirm;
        if (question && !window.confirm(question)) {
            event.preventDefault();
            return;
        }
        form.dataset.busy = "true";
        form.setAttribute("aria-busy", "true");
        form.querySelectorAll("button[type='submit']").forEach(button => {
            button.classList.add("is-submitting");
            button.setAttribute("aria-disabled", "true");
        });
    });
});
window.addEventListener("pageshow", () => {
    document.querySelectorAll("form[data-submit-once]").forEach(form => {
        delete form.dataset.busy;
        form.removeAttribute("aria-busy");
        form.querySelectorAll(".is-submitting").forEach(button => {
            button.classList.remove("is-submitting");
            button.removeAttribute("aria-disabled");
        });
    });
});
function updateCountdown() {
    document.querySelectorAll("[data-countdown]").forEach(element => {
        const deadline = Date.parse(element.dataset.deadline);
        if (!Number.isFinite(deadline)) { element.textContent = "theo thời hạn của vé"; return; }
        const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
        element.textContent = seconds === 0 ? "đã hết hạn" :
            `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
    });
}
updateCountdown();
setInterval(updateCountdown, 1000);
document.querySelectorAll("[data-print]").forEach(button => {
    button.addEventListener("click", () => window.print());
});

// Payment choice is remembered only for this browser session, never treated as proof of payment.
document.querySelectorAll('[data-payment-flow]').forEach(form => {
    const panels = Array.from(form.querySelectorAll('[data-payment-panel]'));
    const currentMethod = () => form.dataset.paymentMethod ||
        form.querySelector('input[name="paymentMethod"]:checked')?.value || 'BANK_TRANSFER';
    const resetQr = panel => {
        panel.querySelectorAll('[data-payment-qr]').forEach(details => {
            details.open = false;
            const image = details.querySelector('[data-qr-src]');
            if (image) image.removeAttribute('src');
            const state = details.querySelector('[data-qr-state]');
            if (state) state.textContent = '';
        });
    };
    const updateLabel = panel => {
        const select = panel.querySelector('[data-channel-select]');
        const label = panel.querySelector('[data-channel-label]');
        if (select && label) label.textContent = select.selectedOptions[0]?.textContent || '';
    };
    panels.forEach(panel => {
        const select = panel.querySelector('[data-channel-select]');
        const key = 'booking.channel.' + panel.dataset.paymentPanel;
        if (select) {
            try {
                const saved = sessionStorage.getItem(key);
                if (saved && Array.from(select.options).some(option => option.value === saved)) select.value = saved;
            } catch (_) { /* Storage is optional, for example in private browsing. */ }
            select.addEventListener('change', () => {
                try { sessionStorage.setItem(key, select.value); } catch (_) { }
                updateLabel(panel);
                resetQr(panel);
            });
        }
        updateLabel(panel);
        panel.querySelectorAll('[data-payment-qr]').forEach(details => {
            details.addEventListener('toggle', () => {
                if (!details.open || panel.hidden) return;
                const image = details.querySelector('[data-qr-src]');
                const state = details.querySelector('[data-qr-state]');
                const url = new URL(image.dataset.qrSrc, window.location.href);
                url.searchParams.set('paymentMethod', currentMethod());
                if (select) url.searchParams.set('channel', select.value);
                if (state) state.textContent = 'Đang tải mã QR…';
                image.onload = () => { if (state) state.textContent = 'Mã thử nghiệm · Không chuyển tiền thật'; };
                image.onerror = () => { if (state) state.textContent = 'Chưa tải được mã. Đóng rồi mở lại để thử.'; };
                image.src = url.href;
            });
        });
    });
    const update = () => {
        const method = currentMethod();
        panels.forEach(panel => {
            const active = panel.dataset.paymentPanel === method;
            panel.hidden = !active;
            panel.querySelectorAll('input, select, button, textarea').forEach(control => { control.disabled = !active; });
            if (!active) resetQr(panel);
        });
        form.dataset.paymentReady = 'true';
    };
    form.querySelectorAll('input[name="paymentMethod"]').forEach(input => input.addEventListener('change', update));
    update();
    window.addEventListener('pageshow', update);
});

const tripFilters = document.querySelector('[data-trip-filters]');
if (tripFilters) {
    const search = tripFilters.querySelector('#trip-search');
    const date = tripFilters.querySelector('#trip-date');
    const cards = Array.from(document.querySelectorAll('[data-trip-card]'));
    const normalize = text => text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
    const filter = () => {
        let shown = 0;
        const query = normalize(search.value.trim());
        cards.forEach(card => {
            card.hidden = !normalize(card.dataset.tripText).includes(query) ||
                (date.value !== '' && card.dataset.tripDate !== date.value);
            if (!card.hidden) shown++;
        });
        tripFilters.querySelector('[data-trip-count]').textContent = `${shown} chuyến phù hợp`;
        document.querySelector('[data-no-trips]').hidden = shown > 0 || cards.length === 0;
    };
    search.addEventListener('input', filter);
    date.addEventListener('change', filter);
    tripFilters.querySelector('[data-clear-trip-filter]').addEventListener('click', () => {
        search.value = ''; date.value = ''; filter(); search.focus();
    });
    filter();
}
