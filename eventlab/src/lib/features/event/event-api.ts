import { apiBaseUrl, ensureSuccess } from "#lib/shared/api/http.ts";
import type { EventDetails } from "./types.ts";

export async function getEvent(): Promise<EventDetails> {
  const response = await fetch(`${apiBaseUrl}/api/event`);
  await ensureSuccess(response);

  return response.json();
}
