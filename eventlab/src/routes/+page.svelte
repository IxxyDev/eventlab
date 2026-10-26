<script lang="ts">
import { onMount } from "svelte";
import { getEvent } from "#lib/features/event/event-api.ts";
import { formatEventSchedule } from "#lib/features/event/event-schedule.ts";
import { EventState } from "#lib/features/event/event-state.svelte.ts";
import RegistrationConfirmation from "#lib/features/registration/components/RegistrationConfirmation.svelte";
import RegistrationForm from "#lib/features/registration/components/RegistrationForm.svelte";
import * as registrationAccess from "#lib/features/registration/registration-access.ts";
import * as registrationApi from "#lib/features/registration/registration-api.ts";
import { RegistrationState } from "#lib/features/registration/registration-state.svelte.ts";

const eventState = new EventState(getEvent);
const registrationState = new RegistrationState({
  api: registrationApi,
  accessStore: registrationAccess,
  event: eventState,
});

onMount(() => {
  void eventState.load();
  void registrationState.restore();
});
</script>

<svelte:head>
  <title>
    {eventState.details ? `${eventState.details.title} – EventLab` : "EventLab"}
  </title>
</svelte:head>

<main>
  {#if eventState.loading}
    <p class="notice" role="status">Загружаем мероприятие…</p>
  {:else if eventState.error && !eventState.details}
    <p class="notice notice--error" role="alert">{eventState.error}</p>
    <button
      class="button button--secondary"
      type="button"
      onclick={() => eventState.load()}
    >
      Попробовать снова
    </button>
  {:else if eventState.details}
    {@const event = eventState.details}
    <p class="eyebrow">Открытая лекция и мастер-класс</p>
    <h1>{event.title}</h1>

    <div class="description">
      <p>{event.description}</p>
    </div>

    <section class="details card" aria-label="Информация для участников">
      <div>
        <h2>Когда и где</h2>
        <p>
          <time datetime={event.startsAt}>{formatEventSchedule(event)}</time>
          по местному времени.
        </p>
        <p>{event.venue}.</p>
      </div>

      <div>
        <h2>Условия участия</h2>
        <p>Всего мест: {event.capacity}.</p>
        <p aria-live="polite">Свободных мест: {event.availableSeats}.</p>
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
  {/if}
</main>

<style>
main {
  max-width: 50rem;
  margin-inline: auto;
  padding: clamp(var(--space-8), 5vw, var(--space-12)) var(--space-4);
}

.eyebrow {
  color: var(--color-text-muted);
  font-weight: 600;
}

h1 {
  margin-block: var(--space-4) var(--space-6);
  font-size: clamp(2rem, 6vw, 3.5rem);
  line-height: 1.1;
}

.description {
  max-width: 65ch;
  font-size: 1.125rem;
}

.details {
  display: grid;
  gap: var(--space-6);
  margin-top: var(--space-8);
}

h2 {
  margin-block: var(--space-6) var(--space-2);
  font-size: 1.125rem;
}

h2:first-child {
  margin-top: 0;
}

p {
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
