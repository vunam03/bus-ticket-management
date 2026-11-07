"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { evaluate } = require("../../main/resources/static/js/password-strength.js");

test("empty input has no strength", () => {
    const result = evaluate("");
    assert.equal(result.level, "empty");
    assert.equal(result.percent, 0);
    assert.equal(result.valid, false);
});
test("short password fails the length rule", () => {
    assert.equal(evaluate("Aa1!").rules.find(rule => rule.key === "length").met, false);
});
test("a valid eight-character password still receives a weak grade", () => {
    const result = evaluate("Aa1!abcd");
    assert.equal(result.valid, true);
    assert.equal(result.level, "weak");
});
test("a varied twelve-character password receives medium", () => {
    assert.equal(evaluate("StrongPass1!").level, "medium");
});
test("a long varied password receives strong", () => {
    const result = evaluate("Xanh!Bien27#Dem88");
    assert.equal(result.level, "strong");
    assert.equal(result.percent, 100);
    assert.equal(result.valid, true);
});
test("common words cannot receive a strong grade", () => {
    assert.equal(evaluate("Password123456!Aa").level, "weak");
});
test("repeated blocks cannot receive a strong grade", () => {
    assert.equal(evaluate("Ab1!Ab1!Ab1!Ab1!").level, "weak");
});
test("spaces and tabs invalidate passwords", () => {
    for (const value of ["Strong Pass1!", "StrongPass1! ", "StrongPass1!\t", "StrongPass1!\u00a0"]) {
        assert.equal(evaluate(value).valid, false);
    }
});
test("UTF-8 bytes are measured independently from character count", () => {
    assert.equal(evaluate("Á".repeat(34) + "a1!B").byteLength, 72);
    assert.equal(evaluate("Á".repeat(34) + "a1!B").valid, true);
    assert.equal(evaluate("Á".repeat(35) + "a1!B").valid, false);
});
test("more than 64 characters cannot be strong", () => {
    const result = evaluate("Aa1!" + "b".repeat(61));
    assert.equal(result.valid, false);
    assert.equal(result.level, "weak");
});
test("conditions explain missing character categories", () => {
    const result = evaluate("abcdefgh");
    assert.equal(result.rules.find(rule => rule.key === "upper").met, false);
    assert.equal(result.rules.find(rule => rule.key === "digit").met, false);
    assert.equal(result.rules.find(rule => rule.key === "special").met, false);
});
