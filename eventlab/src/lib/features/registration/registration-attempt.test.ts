import { describe, expect, it } from "vitest";
import { reuseOrStartAttempt } from "./registration-attempt.ts";

const request = { name: "Анна", email: "anna@example.com" };

describe("reuseOrStartAttempt", () => {
  it("should start an attempt with a UUID key when nothing is pending", () => {
    const attempt = reuseOrStartAttempt(null, request);

    expect(attempt.request).toEqual(request);
    expect(attempt.idempotencyKey).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/,
    );
  });

  it("should reuse the pending attempt when the same data is sent again", () => {
    const pending = reuseOrStartAttempt(null, request);

    const retry = reuseOrStartAttempt(pending, { ...request });

    expect(retry.idempotencyKey).toEqual(pending.idempotencyKey);
  });

  it.each([
    { field: "name", changed: { ...request, name: "Анна Петрова" } },
    { field: "email", changed: { ...request, email: "anna@example.org" } },
  ])("should start a new attempt when $field changes", ({ changed }) => {
    const pending = reuseOrStartAttempt(null, request);

    const next = reuseOrStartAttempt(pending, changed);

    expect(next.idempotencyKey).not.toEqual(pending.idempotencyKey);
    expect(next.request).toEqual(changed);
  });
});
