import { getEvent } from "#lib/features/event/event-api.ts";
import type { PageLoad } from "./$types";

export const load: PageLoad = async ({ fetch, params }) => ({
  event: await getEvent(fetch, params.id),
});
