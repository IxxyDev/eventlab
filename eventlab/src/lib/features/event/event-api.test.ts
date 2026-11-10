import { describe, expect, it, vi } from "vitest";
import { getEvent, getEvents } from "./event-api.ts";

describe("Event API", () => {
  describe("#getEvent", () => {
    it("should request the event by its escaped id", async () => {
      const fetchMock = mockFetch();
      vi.stubGlobal("fetch", fetchMock);

      await getEvent("../registrations");

      expect(fetchMock.mock.calls).toEqual([
        ["http://localhost:8080/api/events/..%2Fregistrations"],
      ]);
    });
  });

  describe("#getEvents", () => {
    it("should request the list of events", async () => {
      const fetchMock = mockFetch();

      await getEvents(fetchMock);

      expect(fetchMock.mock.calls).toEqual([
        ["http://localhost:8080/api/events"],
      ]);
    });
  });

  function mockFetch() {
    return vi.fn<typeof fetch>().mockResolvedValue(Response.json(null));
  }
});
