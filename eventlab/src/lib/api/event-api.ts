import type {
  EventDetails,
  Registration,
  RegistrationCreatedResponse,
  RegistrationRequest,
} from "./types";

const apiBaseUrl = "http://localhost:8080";

export class HttpError extends Error {
  readonly status: number;

  constructor(status: number) {
    super(`HTTP ${status}`);
    this.name = "HttpError";
    this.status = status;
  }
}

export async function getEvent(): Promise<EventDetails> {
  const response = await fetch(`${apiBaseUrl}/api/event`);
  ensureSuccess(response);

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

  ensureSuccess(response);

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

  ensureSuccess(response);

  return response.json();
}

function ensureSuccess(response: Response): void {
  if (!response.ok) {
    throw new HttpError(response.status);
  }
}
