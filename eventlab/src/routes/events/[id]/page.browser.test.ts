import { beforeEach, describe, expect, it, vi } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import type { EventDetails } from "#lib/features/event/types.ts";
import { saveRegistrationAccess } from "#lib/features/registration/registration-access.ts";
import Page from "./+page.svelte";

const access = {
  id: "9c55e2a7-3f6c-4b9e-8a41-0d7f2b1c6e30",
  accessToken: "q7Vd2LxN9tRb4KsY1mPz8WcF3hJg6TaE0uQoXi5nDwM",
};

describe("Event page", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("should load the event from the address", async () => {
    const fetchMock = mockFetch(respondWithEvent({ id: "stage-lighting" }));

    renderComponent("stage-lighting");

    await expect
      .poll(() => fetchMock.mock.calls)
      .toEqual([["http://localhost:8080/api/events/stage-lighting"]]);
  });

  it("should link back to the catalog", async () => {
    mockFetch(respondWithEvent());

    renderComponent();

    await expect
      .element(page.getByRole("link", { name: "Все мероприятия" }))
      .toHaveAttribute("href", "/");
  });

  it("should register for the event from the address", async () => {
    const fetchMock = mockFetch(
      respondWithEvent({ id: "stage-lighting" }),
      respondWithRegistration,
      keepWaiting,
    );
    renderComponent("stage-lighting");

    await registerAnna();

    const [, [address]] = fetchMock.mock.calls;
    expect(address).toEqual(
      "http://localhost:8080/api/events/stage-lighting/registrations",
    );
  });

  it("should say that registration is closed once the event has started", async () => {
    mockFetch(respondWithEvent({ registrationOpen: false }));

    renderComponent();

    await expect
      .element(page.getByText("Регистрация закрыта: мероприятие уже началось."))
      .toBeVisible();
  });

  it("should say that registration is closed when no seats are left", async () => {
    mockFetch(respondWithEvent({ availableSeats: 0 }));

    renderComponent();

    await expect
      .element(page.getByText("Свободные места закончились. Запись закрыта."))
      .toBeVisible();
  });

  it("should offer to try again when the event cannot be loaded", async () => {
    mockFetch(loseResponse);

    renderComponent();

    await expect
      .element(page.getByRole("button", { name: "Попробовать снова" }))
      .toBeVisible();
  });

  it("should show the event after trying again", async () => {
    mockFetch(
      loseResponse,
      respondWithEvent({ title: "Как устроен звук на концерте" }),
    );
    renderComponent();

    await page.getByRole("button", { name: "Попробовать снова" }).click();

    await expect
      .element(
        page.getByRole("heading", { name: "Как устроен звук на концерте" }),
      )
      .toBeVisible();
  });

  it("should show that seats are being refreshed after registration", async () => {
    mockFetch(respondWithEvent(), respondWithRegistration, keepWaiting);
    renderComponent();

    await registerAnna();

    await expect
      .element(page.getByText("Обновляем количество мест…"))
      .toBeVisible();
  });

  it("should offer to refresh seats when the refresh fails", async () => {
    mockFetch(respondWithEvent(), respondWithRegistration, loseResponse);
    renderComponent();

    await registerAnna();

    await expect
      .element(page.getByRole("button", { name: "Обновить количество мест" }))
      .toBeVisible();
  });

  it("should offer to recheck the registration when the server cannot be reached", async () => {
    saveRegistrationAccess("sound-workshop", access);
    mockFetch(respondWithEvent(), loseResponse);

    renderComponent("sound-workshop");

    await expect
      .element(
        page.getByRole("button", { name: "Повторить проверку регистрации" }),
      )
      .toBeVisible();
  });
});

type RespondToRequest = () => Promise<Response>;

function mockFetch(...responses: RespondToRequest[]) {
  const fetchMock = vi.fn<typeof fetch>();

  for (const respond of responses) {
    fetchMock.mockImplementationOnce(respond);
  }

  vi.stubGlobal("fetch", fetchMock);

  return fetchMock;
}

function renderComponent(eventId = "sound-workshop") {
  render(Page, { params: { id: eventId }, data: {} });
}

function respondWithEvent(
  overrides: Partial<EventDetails> = {},
): RespondToRequest {
  const event: EventDetails = {
    id: "sound-workshop",
    title: "Как устроен звук на концерте",
    description: "Разберём путь звука от микрофона до колонок.",
    startsAt: "2026-11-14T15:00:00Z",
    endsAt: "2026-11-14T17:00:00Z",
    timeZone: "Europe/Moscow",
    venue: "Пространство «Сцена», зал № 2",
    capacity: 20,
    availableSeats: 20,
    registrationOpen: true,
    ...overrides,
  };

  return async () => Response.json(event);
}

async function respondWithRegistration(): Promise<Response> {
  return Response.json({
    registration: {
      id: access.id,
      name: "Анна",
      email: "anna@example.com",
    },
    accessToken: access.accessToken,
  });
}

async function loseResponse(): Promise<Response> {
  throw new TypeError("Failed to fetch");
}

function keepWaiting(): Promise<Response> {
  return new Promise(() => {});
}

async function registerAnna() {
  await page.getByRole("textbox", { name: "Имя" }).fill("Анна");
  await page.getByRole("textbox", { name: "Email" }).fill("anna@example.com");
  await page.getByRole("button", { name: "Записаться" }).click();
}
