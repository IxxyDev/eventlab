const storageKey = "eventlab:registration-access:sound-workshop";

export function readRegistrationAccess(): RegistrationAccess | null {
  const saved = localStorage.getItem(storageKey);

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

export function saveRegistrationAccess(access: RegistrationAccess): void {
  localStorage.setItem(storageKey, JSON.stringify(access));
}

export function clearRegistrationAccess(): void {
  localStorage.removeItem(storageKey);
}

export type RegistrationAccess = {
  id: string;
  accessToken: string;
};
