import { resolve } from "$app/paths";
import type { RequestHandler } from "./$types";

export const GET: RequestHandler = ({ url }) => {
  const sitemapUrl = new URL(resolve("/sitemap.xml"), url).href;

  return new Response(
    `User-agent: *
Disallow:
Sitemap: ${sitemapUrl}
`,
    { headers: { "Content-Type": "text/plain; charset=utf-8" } },
  );
};
