import { beforeEach, describe, expect, it } from "vitest";
import {
  clearRegistrationAccess,
  readRegistrationAccess,
  saveRegistrationAccess,
} from "./registration-access.ts";

const storageKey = "eventlab:registration-access:sound-workshop";
const access = { id: "registration-1", accessToken: "token-1" };

describe("registration access", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("should return null when nothing is saved", () => {
    const restored = readRegistrationAccess();

    expect(restored).toEqual(null);
  });

  it("should restore the saved access", () => {
    saveRegistrationAccess(access);

    const restored = readRegistrationAccess();

    expect(restored).toEqual(access);
  });

  it("should forget the access after clearing", () => {
    saveRegistrationAccess(access);

    clearRegistrationAccess();

    expect(readRegistrationAccess()).toEqual(null);
  });

  it.each([
    { saved: "not json" },
    { saved: "null" },
    { saved: '{"id":"registration-1"}' },
    { saved: '{"id":"","accessToken":"token-1"}' },
    { saved: '{"id":1,"accessToken":"token-1"}' },
  ])("should reject corrupted saved value $saved", ({ saved }) => {
    localStorage.setItem(storageKey, saved);

    const restore = () => readRegistrationAccess();

    expect(restore).toThrow();
  });
});
