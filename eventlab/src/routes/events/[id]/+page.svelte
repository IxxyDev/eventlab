<script lang="ts">
import { onMount, untrack } from "svelte";
import EventTimeAndPlace from "#lib/features/event/components/EventTimeAndPlace.svelte";
import { getEvent } from "#lib/features/event/event-api.ts";
import { EventState } from "#lib/features/event/event-state.svelte.ts";
import RegistrationConfirmation from "#lib/features/registration/components/RegistrationConfirmation.svelte";
import RegistrationForm from "#lib/features/registration/components/RegistrationForm.svelte";
import * as registrationAccess from "#lib/features/registration/registration-access.ts";
import * as registrationApi from "#lib/features/registration/registration-api.ts";
import { RegistrationState } from "#lib/features/registration/registration-state.svelte.ts";
import { resolve } from "$app/paths";
import type { PageProps } from "./$types";

const { data }: PageProps = $props();

// The state belongs to one event, so it keeps the event the page was opened with.
const openedEvent = untrack(() => data.event);
const eventState = new EventState(openedEvent, () =>
  getEvent(fetch, openedEvent.id),
);
const event = $derived(eventState.details);
const registrationState = new RegistrationState(openedEvent.id, {
  api: registrationApi,
  accessStore: registrationAccess,
  event: eventState,
});

onMount(() => {
  void registrationState.restore();
});
</script>

<svelte:head>
  <title>{event.title} – EventLab</title>
</svelte:head>

<a href={resolve("/")}>Все мероприятия</a>

<h1 class="title">{event.title}</h1>

<p class="description">{event.description}</p>

<section class="details card" aria-label="Информация для участников">
  <div>
    <h2 class="heading">Когда и где</h2>
    <EventTimeAndPlace {event} />
  </div>

  <div>
    <h2 class="heading">Условия участия</h2>
    <p class="seats">Всего мест: {event.capacity}.</p>
    <p class="seats" aria-live="polite">
      Свободных мест: {event.availableSeats}.
    </p>
  </div>
</section>

{#if eventState.refreshing}
  <p class="notice" role="status">Обновляем количество мест…</p>
{:else if eventState.error}
  <p class="notice notice--warning" role="alert">{eventState.error}</p>
  <button
    class="button button--secondary"
    type="button"
    onclick={() => eventState.load()}
  >
    Обновить количество мест
  </button>
{/if}
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
.title {
  margin-block: var(--space-4) var(--space-6);
  font-size: clamp(2rem, 6vw, 3.5rem);
  line-height: 1.1;
}

.description {
  max-width: 65ch;
  margin-block: var(--space-2);
  font-size: 1.125rem;
}

.details {
  display: grid;
  gap: var(--space-6);
  margin-top: var(--space-8);
}

.heading {
  margin-block: 0 var(--space-2);
  font-size: 1.125rem;
}

.seats,
.notice {
  margin-block: var(--space-2);
}

.storage-note {
  margin-block: var(--space-6);
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

@media (min-width: 40rem) {
  .details {
    grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  }
}
</style>
