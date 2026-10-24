import type { RegistrationRequest } from "./api/types";

export function reuseOrStartAttempt(
  pending: RegistrationAttempt | null,
  request: RegistrationRequest,
): RegistrationAttempt {
  if (
    pending !== null &&
    pending.request.name === request.name &&
    pending.request.email === request.email
  ) {
    return pending;
  }

  return { request, idempotencyKey: crypto.randomUUID() };
}

export type RegistrationAttempt = {
  request: RegistrationRequest;
  idempotencyKey: string;
};
