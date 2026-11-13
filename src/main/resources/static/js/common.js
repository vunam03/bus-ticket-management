/* Tiện ích dùng chung cho index.html và search-results.html */
(function () {
  'use strict';

  const ctxMeta = document.querySelector('meta[name="ctx"]');
  const ctx = ctxMeta ? ctxMeta.content : '/';

  /** Ghép context-path với đường dẫn, ví dụ url('/search') */
  function url(path) {
    return ctx.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
  }

  /** Chống chèn HTML khi dựng giao diện từ dữ liệu JSON */
  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function formatVnd(value) {
    return Number(value).toLocaleString('vi-VN') + 'đ';
  }

  /** yyyy-MM-dd theo giờ máy người dùng (không dùng toISOString vì lệch múi giờ) */
  function toIsoDate(date) {
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return date.getFullYear() + '-' + m + '-' + d;
  }

  function addDays(date, days) {
    const copy = new Date(date.getTime());
    copy.setDate(copy.getDate() + days);
    return copy;
  }

  /** GET JSON; lỗi HTTP => ném Error có message tiếng Việt lấy từ {"error": "..."} */
  async function getJson(path, options) {
    const response = await fetch(path, Object.assign({ headers: { Accept: 'application/json' } }, options));
    let body = null;
    try { body = await response.json(); } catch (ignored) { /* không phải JSON */ }
    if (!response.ok) {
      throw new Error((body && body.error) || 'Có lỗi xảy ra (' + response.status + '). Vui lòng thử lại.');
    }
    return body;
  }

  function fillDatalist(list, values) {
    list.innerHTML = '';
    (values || []).forEach(function (value) {
      const option = document.createElement('option');
      option.value = value;
      list.appendChild(option);
    });
  }

  /**
   * Nạp gợi ý điểm đi/đến vào <datalist>; khi đổi điểm đi thì thu hẹp danh sách điểm đến.
   * Nếu API lỗi thì bỏ qua (ô nhập vẫn gõ tay được).
   */
  function setupLocationLists(originInput, destinationInput, originList, destinationList) {
    getJson(url('/search/locations')).then(function (data) {
      fillDatalist(originList, data.origins);
      fillDatalist(destinationList, data.destinations);
    }).catch(function () { /* bỏ qua */ });

    originInput.addEventListener('change', function () {
      getJson(url('/search/destinations?origin=' + encodeURIComponent(originInput.value.trim())))
        .then(function (list) { fillDatalist(destinationList, list); })
        .catch(function () { /* bỏ qua */ });
    });
  }

  window.BusUtils = {
    url: url, escapeHtml: escapeHtml, formatVnd: formatVnd,
    toIsoDate: toIsoDate, addDays: addDays, getJson: getJson,
    setupLocationLists: setupLocationLists
  };
})();
