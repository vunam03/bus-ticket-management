/* Trang kết quả: lọc / sắp xếp / phân trang bằng AJAX (không tải lại trang) */
(function () {
  'use strict';
  const U = window.BusUtils;
  const app = document.getElementById('resultsApp');
  if (!app) return;

  // ---------- Trạng thái ----------
  const state = {
    origin: app.dataset.origin || '',
    destination: app.dataset.destination || '',
    travelDate: app.dataset.travelDate || '',
    sortBy: app.dataset.sortBy || 'PRICE_ASC',
    page: 1,
    size: 10
  };
  const filterUrl = app.dataset.filterUrl;
  const searchUrl = app.dataset.searchUrl;
  const bookingUrl = app.dataset.bookingUrl;

  const el = {
    form: document.getElementById('filterForm'),
    list: document.getElementById('tripList'),
    count: document.getElementById('resultCount'),
    alert: document.getElementById('alertBox'),
    pagination: document.getElementById('pagination'),
    minPrice: document.getElementById('minPrice'),
    maxPrice: document.getElementById('maxPrice'),
    minOut: document.getElementById('minPriceOut'),
    maxOut: document.getElementById('maxPriceOut'),
    sortTabs: document.querySelectorAll('.sort-tab'),
    strip: document.getElementById('dateStrip'),
    sidebar: document.getElementById('sidebar'),
    toggle: document.getElementById('sidebarToggle'),
    filterBadge: document.getElementById('filterBadge')
  };

  let controller = null;   // huỷ yêu cầu cũ khi có yêu cầu mới
  let timer = null;        // debounce thanh trượt

  // ---------- Dựng query string từ trạng thái + sidebar ----------
  function buildParams() {
    const p = new URLSearchParams();
    if (state.origin) p.set('origin', state.origin);
    if (state.destination) p.set('destination', state.destination);
    if (state.travelDate) p.set('travelDate', state.travelDate);

    // Chỉ gửi khoảng giá khi người dùng thật sự thu hẹp so với biên của thanh trượt
    if (Number(el.minPrice.value) > Number(el.minPrice.min)) p.set('minPrice', el.minPrice.value);
    if (Number(el.maxPrice.value) < Number(el.maxPrice.max)) p.set('maxPrice', el.maxPrice.value);

    el.form.querySelectorAll('input[name="busTypes"]:checked').forEach(function (i) { p.append('busTypes', i.value); });
    el.form.querySelectorAll('input[name="timeSlots"]:checked').forEach(function (i) { p.append('timeSlots', i.value); });
    const rating = el.form.querySelector('input[name="minRating"]:checked');
    if (rating && rating.value) p.set('minRating', rating.value);

    p.set('sortBy', state.sortBy);
    p.set('page', state.page);
    p.set('size', state.size);
    return p;
  }

  // ---------- Gọi API ----------
  function load(options) {
    options = options || {};
    if (controller) controller.abort();
    controller = new AbortController();
    const params = buildParams();
    el.list.classList.add('is-loading');
    showError('');

    U.getJson(filterUrl + '?' + params.toString(), { signal: controller.signal })
      .then(function (data) {
        render(data, options);
        syncUrl(params);
        if (options.scroll) app.scrollIntoView({ behavior: 'smooth', block: 'start' });
      })
      .catch(function (err) {
        if (err.name === 'AbortError') return;
        showError(err.message);
      })
      .finally(function () { el.list.classList.remove('is-loading'); });
  }

  function syncUrl(params) {
    const clean = new URLSearchParams(params);
    clean.delete('size');
    if (clean.get('page') === '1') clean.delete('page');
    history.replaceState(null, '', searchUrl + '?' + clean.toString());
  }

  function showError(message) {
    el.alert.hidden = !message;
    el.alert.textContent = message || '';
  }

  // ---------- Hiển thị kết quả ----------
  function tripCard(t) {
    const badges = (t.badges || []).map(function (b) { return '<span class="m3-badge">' + U.escapeHtml(b) + '</span>'; }).join('');
    return '<article class="ticket">' +
      '<div class="ticket-main">' +
        '<div class="ticket-head"><h3 class="company">' + U.escapeHtml(t.busCompany) + '</h3>' +
          '<span class="chip">' + U.escapeHtml(t.busTypeLabel) + '</span>' + badges + '</div>' +
        '<div class="trip-line">' +
          '<div class="trip-time"><strong>' + U.escapeHtml(t.departureTimeText) + '</strong>' +
            '<span>' + U.escapeHtml(t.dayOfWeekText) + ', ' + U.escapeHtml(t.dateText) + '</span></div>' +
          '<div class="trip-places"><span class="place">' + U.escapeHtml(t.origin) + '</span><span class="arrow" aria-hidden="true"></span>' +
            '<span class="place">' + U.escapeHtml(t.destination) + '</span></div>' +
        '</div>' +
        '<div class="ticket-meta">' +
          '<span class="rating">★ ' + Number(t.rating).toFixed(1) + ' nhà xe</span>' +
          '<span class="' + (t.lowSeats ? 'low' : '') + '">' + U.escapeHtml(t.seatsText) + '</span>' +
          '<span>Giờ đi: ' + U.escapeHtml(t.timeSlotLabel) + '</span>' +
        '</div>' +
      '</div>' +
      '<div class="ticket-stub">' +
        '<div class="price">' + U.escapeHtml(t.priceText) + '</div><div class="per">mỗi khách</div>' +
        (t.soldOut
          ? '<span class="m3-btn m3-btn-ghost" aria-disabled="true">Hết chỗ</span>'
          : '<a class="m3-btn m3-btn-primary" href="' + bookingUrl + '/' + encodeURIComponent(t.id) + '/seats">Chọn chuyến</a>') +
      '</div></article>';
  }

  function emptyState() {
    return '<div class="empty-state"><h3>Không có chuyến phù hợp</h3>' +
      '<p>Thử nới khoảng giá, bỏ bớt loại xe hoặc khung giờ, hoặc chọn ngày khác.</p>' +
      '<button type="button" class="m3-btn m3-btn-dark" data-action="reset">Xoá bộ lọc</button></div>';
  }

  function render(data, options) {
    el.list.innerHTML = data.trips.length ? data.trips.map(tripCard).join('') : emptyState();
    el.count.innerHTML = '<strong>' + data.totalItems + '</strong> chuyến xe phù hợp' +
      (data.totalBeforeFilters !== data.totalItems ? ' (trên tổng ' + data.totalBeforeFilters + ' chuyến)' : '');
    updateFacets(data);
    updateSliderBounds(data);
    renderPagination(data.page, data.totalPages);
    el.sortTabs.forEach(function (tab) { tab.setAttribute('aria-pressed', String(tab.dataset.sort === data.sortBy)); });
    el.filterBadge.textContent = data.activeFilterCount ? '(' + data.activeFilterCount + ')' : '';
  }

  function updateFacets(data) {
    (data.busTypeFacets || []).concat(data.timeSlotFacets || []).forEach(function (f) {
      document.querySelectorAll('[data-facet="' + f.code + '"]').forEach(function (node) {
        node.textContent = f.count;
        const row = node.closest('.check');
        if (row) row.classList.toggle('is-empty', f.count === 0);
      });
    });
  }

  function updateSliderBounds(data) {
    const min = Number(data.priceMin), max = Number(data.priceMax);
    const changed = Number(el.minPrice.min) !== min || Number(el.maxPrice.max) !== max;
    if (!changed) return;
    [el.minPrice, el.maxPrice].forEach(function (r) { r.min = min; r.max = max; });
    el.minPrice.value = min;   // biên đổi (vd. đổi ngày) => đặt lại toàn khoảng
    el.maxPrice.value = max;
    syncPriceLabels();
  }

  function renderPagination(page, totalPages) {
    el.pagination.innerHTML = '';
    if (totalPages <= 1) return;
    function button(label, target, opts) {
      opts = opts || {};
      const b = document.createElement('button');
      b.type = 'button';
      b.textContent = label;
      b.disabled = !!opts.disabled;
      if (opts.current) b.setAttribute('aria-current', 'page');
      if (opts.aria) b.setAttribute('aria-label', opts.aria);
      b.addEventListener('click', function () { state.page = target; load({ scroll: true }); });
      el.pagination.appendChild(b);
    }
    button('‹', page - 1, { disabled: page <= 1, aria: 'Trang trước' });
    const from = Math.max(1, page - 2), to = Math.min(totalPages, page + 2);
    for (let p = from; p <= to; p++) button(String(p), p, { current: p === page });
    button('›', page + 1, { disabled: page >= totalPages, aria: 'Trang sau' });
  }

  // ---------- Thanh trượt giá ----------
  function syncPriceLabels() {
    el.minOut.textContent = U.formatVnd(el.minPrice.value);
    el.maxOut.textContent = U.formatVnd(el.maxPrice.value);
  }

  function onPriceInput(event) {
    // Không cho khoảng giá bị ngược (min > max)
    if (Number(el.minPrice.value) > Number(el.maxPrice.value)) {
      if (event.target === el.minPrice) el.maxPrice.value = el.minPrice.value;
      else el.minPrice.value = el.maxPrice.value;
    }
    syncPriceLabels();
    clearTimeout(timer);
    timer = setTimeout(function () { state.page = 1; load(); }, 300);
  }
  el.minPrice.addEventListener('input', onPriceInput);
  el.maxPrice.addEventListener('input', onPriceInput);

  // ---------- Sự kiện sidebar / sắp xếp / ngày ----------
  el.form.addEventListener('change', function (event) {
    if (event.target.type === 'checkbox' || event.target.type === 'radio') {
      state.page = 1;
      load();
    }
  });
  el.form.addEventListener('submit', function (event) { event.preventDefault(); });

  function resetFilters() {
    el.form.querySelectorAll('input[type=checkbox]').forEach(function (c) { c.checked = false; });
    const any = el.form.querySelector('input[name="minRating"][value=""]');
    if (any) any.checked = true;
    el.minPrice.value = el.minPrice.min;
    el.maxPrice.value = el.maxPrice.max;
    syncPriceLabels();
    state.page = 1;
    load();
  }
  document.getElementById('resetBtn').addEventListener('click', resetFilters);
  el.list.addEventListener('click', function (event) {
    if (event.target.closest('[data-action="reset"]')) resetFilters();
  });

  el.sortTabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      state.sortBy = tab.dataset.sort;
      state.page = 1;
      load();
    });
  });

  if (el.strip) {
    el.strip.addEventListener('click', function (event) {
      const day = event.target.closest('.day');
      if (!day) return;
      state.travelDate = day.dataset.date;
      state.page = 1;
      el.strip.querySelectorAll('.day').forEach(function (d) { d.classList.toggle('is-active', d === day); });
      load({ resetPrice: true });
    });
  }

  if (el.toggle) {
    el.toggle.addEventListener('click', function () {
      const open = el.sidebar.classList.toggle('is-open');
      el.toggle.setAttribute('aria-expanded', String(open));
    });
  }

  // ---------- Khởi tạo ----------
  syncPriceLabels();
  renderPagination(Number(el.pagination.dataset.page || 1), Number(el.pagination.dataset.totalPages || 1));
  // Gợi ý địa điểm cho thanh tìm kiếm phía trên
  const o = document.getElementById('origin'), d = document.getElementById('destination');
  if (o && d) U.setupLocationLists(o, d, document.getElementById('originList'), document.getElementById('destinationList'));
})();
