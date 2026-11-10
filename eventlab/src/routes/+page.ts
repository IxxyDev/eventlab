import { getEvents } from "#lib/features/event/event-api.ts";
import type { PageLoad } from "./$types";

export const load: PageLoad = async ({ fetch }) => ({
  events: await getEvents(fetch),
});
