import { expect, test } from "@playwright/test";

test("should advertise the sitemap in robots.txt", async ({
  request,
  baseURL,
}) => {
  const sitemapUrl = new URL("/sitemap.xml", baseURL).href;

  const response = await request.get("/robots.txt");

  expect(response.status()).toEqual(200);
  expect(await response.text()).toEqual(
    `User-agent: *\nDisallow:\nSitemap: ${sitemapUrl}\n`,
  );
});

test("should list the catalog and event pages in the sitemap", async ({
  request,
  page,
  baseURL,
}) => {
  const response = await request.get("/sitemap.xml");
  const xml = await response.text();
  const sitemap = await page.evaluate((source) => {
    const sitemapDocument = new DOMParser().parseFromString(
      source,
      "application/xml",
    );

    return {
      namespace: sitemapDocument.documentElement.namespaceURI,
      errors: sitemapDocument.getElementsByTagName("parsererror").length,
      urls: Array.from(
        sitemapDocument.querySelectorAll("urlset > url > loc"),
        (element) => element.textContent,
      ).sort(),
    };
  }, xml);

  expect(response.status()).toEqual(200);
  expect(sitemap).toEqual({
    namespace: "http://www.sitemaps.org/schemas/sitemap/0.9",
    errors: 0,
    urls: [
      new URL("/", baseURL).href,
      new URL("/events/sound-workshop", baseURL).href,
      new URL("/events/stage-lighting", baseURL).href,
    ],
  });
});
