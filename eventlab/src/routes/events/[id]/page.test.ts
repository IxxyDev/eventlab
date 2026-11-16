import { describe, expect, it, vi } from "vitest";
import { load } from "./+page.ts";
import type { PageLoadEvent } from "./$types";

describe("Event page", () => {
  describe("#load", () => {
    it("should load the event from the address", async () => {
      const fetchMock = mockFetch();

      await loadPage("stage-lighting", fetchMock);

      expect(fetchMock.mock.calls).toEqual([
        ["http://localhost:8080/api/events/stage-lighting"],
      ]);
    });
  });

  function mockFetch() {
    return vi.fn<typeof fetch>().mockResolvedValue(Response.json(null));
  }

  function loadPage(id: string, fetch: typeof globalThis.fetch) {
    return load({ fetch, params: { id } } as PageLoadEvent);
  }
});
