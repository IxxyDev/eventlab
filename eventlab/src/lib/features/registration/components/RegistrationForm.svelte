<script lang="ts">
import type { RegistrationRequest } from "../types.ts";

type Props = {
  submitting: boolean;
  registrationError: string;
  onRegister: (request: RegistrationRequest) => Promise<void>;
};

let { submitting, registrationError, onRegister }: Props = $props();

let name = $state("");
let email = $state("");

async function handleSubmit(e: SubmitEvent) {
  e.preventDefault();

  if (submitting) {
    return;
  }

  await onRegister({ name, email });
}
</script>

<form
  class="card"
  onsubmit={handleSubmit}
  aria-busy={submitting}
  aria-labelledby="registration-heading"
  aria-describedby={registrationError ? "registration-error" : undefined}
>
  <h2 id="registration-heading">Запись на мероприятие</h2>

  <label>
    Имя
    <input
      name="name"
      autocomplete="name"
      bind:value={name}
      required
      pattern=".*\S.*"
      title="Введите имя, содержащее не только пробелы"
      disabled={submitting}
      maxlength={100}
    >
  </label>

  <label>
    Email
    <input
      type="email"
      name="email"
      autocomplete="email"
      bind:value={email}
      required
      disabled={submitting}
      maxlength={254}
    >
  </label>

  {#if registrationError}
    <p id="registration-error" class="notice notice--error" role="alert">
      {registrationError}
    </p>
  {/if}

  <button class="button" type="submit" disabled={submitting}>
    {submitting ? "Отправляем заявку…" : "Записаться"}
  </button>
</form>

<style>
p {
  margin: 0;
}

form {
  display: grid;
  gap: var(--space-4);
  margin-top: var(--space-8);
}

h2 {
  margin: 0;
  font-size: 1.5rem;
  line-height: 1.3;
}

label {
  display: grid;
  gap: var(--space-2);
  font-weight: 600;
}

input {
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid var(--color-input-border);
  border-radius: var(--radius-control);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-weight: 400;
}

input:disabled {
  background: var(--color-background);
  cursor: not-allowed;
}

input:focus-visible {
  outline: 3px solid var(--color-focus);
  outline-offset: 3px;
}
</style>
