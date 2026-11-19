<script lang="ts">
import { onMount } from "svelte";
import type { RegistrationState } from "../registration-state.svelte.ts";
import RegistrationConfirmation from "./RegistrationConfirmation.svelte";
import RegistrationForm from "./RegistrationForm.svelte";

interface Props {
  registrationState: RegistrationState;
  event: { registrationOpen: boolean; availableSeats: number };
}

const { registrationState, event }: Props = $props();

onMount(() => {
  void registrationState.restore();
});
</script>

<p class="storage-note">
  Заявка хранится на сервере. Ключ доступа сохраняется в этом браузере.
</p>

{#if registrationState.cancelNotice}
  <p class="notice" role="status">{registrationState.cancelNotice}</p>
{/if}

{#if registrationState.storageWarning}
  <p class="notice notice--warning" role="status">
    {registrationState.storageWarning}
  </p>
{/if}

{#if registrationState.restoreState.status === "loading"}
  <p class="notice" role="status">Проверяем вашу регистрацию…</p>
{:else if registrationState.restoreState.status === "error"}
  <p class="notice notice--error" role="alert">
    {registrationState.restoreState.message}
  </p>

  <button
    class="button button--secondary"
    type="button"
    onclick={() => registrationState.restore()}
  >
    Повторить проверку регистрации
  </button>
{:else if registrationState.registration}
  <RegistrationConfirmation
    registration={registrationState.registration}
    cancelling={registrationState.cancelling}
    cancelError={registrationState.cancelError}
    onCancel={() => registrationState.cancel()}
  />
{:else if !event.registrationOpen}
  <p class="notice" role="status">
    Регистрация закрыта: мероприятие уже началось.
  </p>
{:else if event.availableSeats === 0}
  <p class="notice" role="status">
    Свободные места закончились. Запись закрыта.
  </p>
{:else}
  <RegistrationForm
    submitting={registrationState.submitting}
    registrationError={registrationState.registrationError}
    onRegister={(request) => registrationState.register(request)}
  />
{/if}

<style>
.storage-note {
  margin-block: var(--space-6);
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.notice {
  margin-block: var(--space-2);
}
</style>
