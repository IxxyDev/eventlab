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
    const restored = readRegistrationAccess("sound-workshop");

    expect(restored).toEqual(null);
  });

  it("should restore the saved access", () => {
    saveRegistrationAccess("sound-workshop", access);

    const restored = readRegistrationAccess("sound-workshop");

    expect(restored).toEqual(access);
  });

  it("should forget the access after clearing", () => {
    saveRegistrationAccess("sound-workshop", access);

    clearRegistrationAccess("sound-workshop");

    expect(readRegistrationAccess("sound-workshop")).toEqual(null);
  });

  it("should not restore the access saved for another event", () => {
    saveRegistrationAccess("sound-workshop", access);

    const restored = readRegistrationAccess("stage-lighting");

    expect(restored).toEqual(null);
  });

  it("should not clear the access saved for another event", () => {
    saveRegistrationAccess("sound-workshop", access);

    clearRegistrationAccess("stage-lighting");

    expect(readRegistrationAccess("sound-workshop")).toEqual(access);
  });

  it.each([
    { saved: "not json" },
    { saved: "null" },
    { saved: '{"id":"registration-1"}' },
    { saved: '{"id":"","accessToken":"token-1"}' },
    { saved: '{"id":1,"accessToken":"token-1"}' },
  ])("should reject corrupted saved value $saved", ({ saved }) => {
    localStorage.setItem(storageKey, saved);

    const restore = () => readRegistrationAccess("sound-workshop");

    expect(restore).toThrow();
  });
});
