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

    it("should omit query parameters from the canonical URL", async () => {
      const url = new URL(
        "http://localhost:5173/events/sound-workshop?utm_source=chat",
      );

      const data = await loadPage("sound-workshop", mockFetch(), url);

      expect(data?.canonicalUrl).toEqual(
        "http://localhost:5173/events/sound-workshop",
      );
    });
  });

  function mockFetch(response = Response.json(null)) {
    return vi.fn<typeof fetch>().mockResolvedValue(response);
  }

  function loadPage(
    id: string,
    fetch: typeof globalThis.fetch,
    url = new URL(`http://localhost:5173/events/${id}`),
  ) {
    return load({ fetch, params: { id }, url } as PageLoadEvent);
  }
});
