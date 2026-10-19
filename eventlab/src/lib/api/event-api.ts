import type {
  EventDetails,
  Registration,
  RegistrationCreatedResponse,
  RegistrationRequest,
} from "./types";

const apiBaseUrl = "http://localhost:8080";

export class HttpError extends Error {
  readonly status: number;
  readonly code: string | undefined;

  constructor(status: number, code?: string) {
    super(`HTTP ${status}`);
    this.name = "HttpError";
    this.status = status;
    this.code = code;
  }
}

export async function getEvent(): Promise<EventDetails> {
  const response = await fetch(`${apiBaseUrl}/api/event`);
  await ensureSuccess(response);

  return response.json();
}

export async function createRegistration(
  request: RegistrationRequest,
): Promise<RegistrationCreatedResponse> {
  const response = await fetch(`${apiBaseUrl}/api/event/registrations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  await ensureSuccess(response);

  return response.json();
}

export async function getRegistration(
  id: string,
  accessToken: string,
): Promise<Registration> {
  const response = await fetch(
    `${apiBaseUrl}/api/registrations/${encodeURIComponent(id)}`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  await ensureSuccess(response);

  return response.json();
}

export async function cancelRegistration(
  id: string,
  accessToken: string,
): Promise<void> {
  const response = await fetch(
    `${apiBaseUrl}/api/registrations/${encodeURIComponent(id)}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  await ensureSuccess(response);
}

async function ensureSuccess(response: Response): Promise<void> {
  if (response.ok) {
    return;
  }

  let code: string | undefined;

  try {
    const body: unknown = await response.json();

    if (
      typeof body === "object" &&
      body !== null &&
      "code" in body &&
      typeof body.code === "string"
    ) {
      code = body.code;
    }
  } catch {
    // Save HTTP error status even without correct JSON
  }

  throw new HttpError(response.status, code);
}
