import { getEvents } from "#lib/features/event/event-api.ts";
import { resolve } from "$app/paths";
import type { RequestHandler } from "./$types";

export const GET: RequestHandler = async ({ fetch, url }) => {
  const events = await getEvents(fetch);
  const paths = [
    resolve("/"),
    ...events.map(({ id }) => resolve("/events/[id]", { id })),
  ];
  const entries = paths
    .map(
      (path) => `<url><loc>${escapeXml(new URL(path, url).href)}</loc></url>`,
    )
    .join("\n");

  return new Response(
    `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${entries}
</urlset>`,
    { headers: { "Content-Type": "application/xml; charset=utf-8" } },
  );
};

function escapeXml(text: string) {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");
}
