import { error } from "@sveltejs/kit";
import { getEvent } from "#lib/features/event/event-api.ts";
import { HttpError } from "#lib/shared/api/http.ts";
import type { PageLoad } from "./$types";

export const load: PageLoad = async ({ fetch, params, url }) => {
  try {
    return {
      event: await getEvent(fetch, params.id),
      canonicalUrl: `${url.origin}${url.pathname}`,
    };
  } catch (cause) {
    if (cause instanceof HttpError && cause.status === 404) {
      error(404);
    }

    throw cause;
  }
};
