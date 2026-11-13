/* Trang chủ: khung tìm kiếm đa năng + tuyến phổ biến */
(function () {
  'use strict';
  const U = window.BusUtils;
  const form = document.getElementById('searchForm');
  if (!form) return;

  const origin = document.getElementById('origin');
  const destination = document.getElementById('destination');
  const travelDate = document.getElementById('travelDate');
  const errorBox = document.getElementById('formError');

  // --- Ngày đi: không cho chọn quá khứ, mặc định là ngày mai ---
  const today = new Date();
  travelDate.min = U.toIsoDate(today);
  travelDate.max = U.toIsoDate(U.addDays(today, 60));
  travelDate.value = U.toIsoDate(U.addDays(today, 1));

  const quickButtons = document.querySelectorAll('[data-day-offset]');
  function markQuickDate() {
    quickButtons.forEach(function (btn) {
      const expected = U.toIsoDate(U.addDays(new Date(), Number(btn.dataset.dayOffset)));
      btn.classList.toggle('is-active', travelDate.value === expected);
    });
  }
  quickButtons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      travelDate.value = U.toIsoDate(U.addDays(new Date(), Number(btn.dataset.dayOffset)));
      markQuickDate();
    });
  });
  travelDate.addEventListener('change', markQuickDate);
  markQuickDate();

  // --- Gợi ý địa điểm + nút đổi chiều ---
  U.setupLocationLists(origin, destination,
    document.getElementById('originList'), document.getElementById('destinationList'));

  document.getElementById('swapBtn').addEventListener('click', function () {
    const tmp = origin.value;
    origin.value = destination.value;
    destination.value = tmp;
  });

  // --- Kiểm tra trước khi gửi ---
  form.addEventListener('submit', function (event) {
    errorBox.textContent = '';
    const from = origin.value.trim().toLowerCase();
    const to = destination.value.trim().toLowerCase();
    if (!from || !to) {
      event.preventDefault();
      errorBox.textContent = 'Vui lòng nhập điểm đi và điểm đến.';
    } else if (from === to) {
      event.preventDefault();
      errorBox.textContent = 'Điểm đi và điểm đến không được trùng nhau.';
    }
  });

  // --- Tuyến phổ biến (gọi /routes) ---
  const grid = document.getElementById('popularRoutes');
  function renderRoutes(routes) {
    if (!routes.length) {
      grid.innerHTML = '<p class="empty-note">Chưa có tuyến nào để gợi ý.</p>';
      return;
    }
    grid.innerHTML = routes.map(function (r) {
      const href = U.url('/routes/detail?origin=' + encodeURIComponent(r.origin) +
        '&destination=' + encodeURIComponent(r.destination));
      return '<a class="route-card" href="' + href + '">' +
        (r.highlight ? '<span class="tag">' + U.escapeHtml(r.highlight) + '</span>' : '') +
        '<h3>' + U.escapeHtml(r.origin) + ' → ' + U.escapeHtml(r.destination) + '</h3>' +
        '<span class="meta">' + r.tripCount + ' chuyến sắp chạy · ★ ' + r.avgRating.toFixed(1) + '</span>' +
        '<span class="from-price">Từ ' + U.escapeHtml(r.minPriceText) + '</span></a>';
    }).join('');
  }
  U.getJson(U.url('/routes?limit=6')).then(renderRoutes).catch(function (err) {
    grid.innerHTML = '<p class="empty-note">Không tải được tuyến phổ biến: ' + U.escapeHtml(err.message) + '</p>';
  });
})();
