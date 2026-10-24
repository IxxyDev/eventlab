import { describe, expect, it, vi } from "vitest";
import {
  cancelRegistration,
  createRegistration,
  getRegistration,
  HttpError,
} from "./event-api";

const request = { name: "Анна", email: "anna@example.com" };
const created = {
  registration: { id: "registration-1", ...request },
  accessToken: "token-1",
};

function stubFetch(response: Response) {
  const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);

  return fetchMock;
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("event API client", () => {
  it("should send the registration as JSON with the idempotency key", async () => {
    const fetchMock = stubFetch(jsonResponse(201, created));

    const result = await createRegistration(request, "key-1");

    expect(fetchMock.mock.calls).toEqual([
      [
        "http://localhost:8080/api/event/registrations",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Idempotency-Key": "key-1",
          },
          body: JSON.stringify(request),
        },
      ],
    ]);
    expect(result).toEqual(created);
  });

  it("should send the access token and escape the registration id", async () => {
    const fetchMock = stubFetch(jsonResponse(200, created.registration));

    const registration = await getRegistration("a/b?c", "token-1");

    expect(fetchMock.mock.calls).toEqual([
      [
        "http://localhost:8080/api/registrations/a%2Fb%3Fc",
        { headers: { Authorization: "Bearer token-1" } },
      ],
    ]);
    expect(registration).toEqual(created.registration);
  });

  it("should cancel with DELETE and accept an empty response", async () => {
    const fetchMock = stubFetch(new Response(null, { status: 204 }));

    await cancelRegistration("registration-1", "token-1");

    expect(fetchMock.mock.calls).toEqual([
      [
        "http://localhost:8080/api/registrations/registration-1",
        {
          method: "DELETE",
          headers: { Authorization: "Bearer token-1" },
        },
      ],
    ]);
  });

  it.each([
    {
      reason: "the API error code",
      response: jsonResponse(409, { code: "EVENT_FULL" }),
      error: new HttpError(409, "EVENT_FULL"),
    },
    {
      reason: "no code for an HTML page from a proxy",
      response: new Response("<h1>Bad Gateway</h1>", { status: 502 }),
      error: new HttpError(502),
    },
    {
      reason: "no code for JSON without a code",
      response: jsonResponse(500, { message: "Internal error" }),
      error: new HttpError(500),
    },
  ])(
    "should reject with HttpError and $reason",
    async ({ response, error }) => {
      stubFetch(response);

      const sending = createRegistration(request, "key-1");

      await expect(sending).rejects.toEqual(error);
    },
  );

  it("should pass a network failure through unchanged", async () => {
    const failure = new TypeError("Failed to fetch");
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockRejectedValue(failure));

    const sending = createRegistration(request, "key-1");

    await expect(sending).rejects.toEqual(failure);
  });
});
