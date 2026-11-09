import { apiBaseUrl, ensureSuccess } from "#lib/shared/api/http.ts";
import type { EventDetails } from "./types.ts";

export async function getEvent(id: string): Promise<EventDetails> {
  const response = await fetch(
    `${apiBaseUrl}/api/events/${encodeURIComponent(id)}`,
  );
  await ensureSuccess(response);

  return response.json();
}

export async function getEvents(
  fetch: typeof globalThis.fetch,
): Promise<EventDetails[]> {
  const response = await fetch(`${apiBaseUrl}/api/events`);
  await ensureSuccess(response);

  return response.json();
}
