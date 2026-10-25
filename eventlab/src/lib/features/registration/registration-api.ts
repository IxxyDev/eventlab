import { apiBaseUrl, ensureSuccess } from "#lib/shared/api/http.ts";
import type {
  Registration,
  RegistrationCreatedResponse,
  RegistrationRequest,
} from "./types.ts";

export async function createRegistration(
  request: RegistrationRequest,
  idempotencyKey: string,
): Promise<RegistrationCreatedResponse> {
  const response = await fetch(`${apiBaseUrl}/api/event/registrations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
    },
    body: JSON.stringify(request),
  });’
дел
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
