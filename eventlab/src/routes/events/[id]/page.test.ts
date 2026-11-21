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

    it("should answer not found when the event does not exist", async () => {
      const fetchMock = mockFetch(new Response(null, { status: 404 }));

      const loading = loadPage("vinyl-mastering", fetchMock);

      await expect(loading).rejects.toHaveProperty("status", 404);
    });
  });

  function mockFetch(response = Response.json(null)) {
    return vi.fn<typeof fetch>().mockResolvedValue(response);
  }

  function loadPage(id: string, fetch: typeof globalThis.fetch) {
    return load({ fetch, params: { id } } as PageLoadEvent);
  }
});
