export function readRegistrationAccess(
  eventId: string,
): RegistrationAccess | null {
  const saved = localStorage.getItem(toStorageKey(eventId));

  if (saved === null) {
    return null;
  }

  const access: unknown = JSON.parse(saved);

  if (
    typeof access !== "object" ||
    access === null ||
    !("id" in access) ||
    !("accessToken" in access) ||
    typeof access.id !== "string" ||
    typeof access.accessToken !== "string" ||
    !access.id ||
    !access.accessToken
  ) {
    throw new Error("Invalid saved registration access");
  }

  return {
    id: access.id,
    accessToken: access.accessToken,
  };
}

export function saveRegistrationAccess(
  eventId: string,
  access: RegistrationAccess,
): void {
  localStorage.setItem(toStorageKey(eventId), JSON.stringify(access));
}

export function clearRegistrationAccess(eventId: string): void {
  localStorage.removeItem(toStorageKey(eventId));
}

function toStorageKey(eventId: string): string {
  return `eventlab:registration-access:${eventId}`;
}

export type RegistrationAccess = {
  id: string;
  accessToken: string;
};
