"use strict";

(function (root, factory) {
    const api = factory();
    if (typeof module === "object" && module.exports) {
        module.exports = api;
    } else {
        root.AccountPasswordStrength = api;
    }
})(globalThis, function () {
    function evaluate(input) {
        const value = String(input ?? "");
        const hasValue = value.length > 0;
        const byteLength = new TextEncoder().encode(value).length;
        const rules = [
            { key: "length", label: "8–64 ký tự", met: value.length >= 8 && value.length <= 64 },
            { key: "upper", label: "chữ hoa", met: /\p{Lu}/u.test(value) },
            { key: "lower", label: "chữ thường", met: /\p{Ll}/u.test(value) },
            { key: "digit", label: "chữ số", met: /\p{Nd}/u.test(value) },
            { key: "special", label: "ký tự đặc biệt", met: /[^\p{L}\p{Nd}\s]/u.test(value) },
            { key: "noWhitespace", label: "không có khoảng trắng", met: hasValue && !/\s/u.test(value) },
            { key: "bytes", label: "tối đa 72 byte UTF-8", met: hasValue && byteLength <= 72 }
        ];
        const valid = rules.every(rule => rule.met);
        const problems = rules.filter(rule => !rule.met).map(rule => rule.label);
        let level = "empty";
        let label = "Chưa nhập mật khẩu";
        let percent = 0;
        if (hasValue) {
            level = "weak";
            label = valid ? "Yếu — nên dùng mật khẩu dài hơn" : "Yếu — chưa đủ điều kiện";
            percent = valid ? 55 : Math.min(45, rules.filter(rule => rule.met).length * 7);
            const predictable = /(password|matkhau|qwerty|123456|abcdef)/i.test(value)
                || /^(.{1,8})\1{1,}$/u.test(value);
            if (valid && !predictable && value.length >= 12) {
                level = value.length >= 16 ? "strong" : "medium";
                label = level === "strong" ? "Mạnh" : "Vừa — tăng độ dài để tốt hơn";
                percent = level === "strong" ? 100 : 75;
            }
        }
        return { valid, problems, rules, level, label, percent, byteLength };
    }
    return Object.freeze({ evaluate });
});
