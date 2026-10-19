<script lang="ts">
import { onMount } from "svelte";
import {
  createRegistration,
  getEvent,
  getRegistration,
  HttpError,
} from "#lib/api/event-api.ts";
import type {
  EventDetails,
  Registration,
  RegistrationRequest,
} from "#lib/api/types.ts";
import RegistrationConfirmation from "#lib/components/RegistrationConfirmation.svelte";
import RegistrationForm from "#lib/components/RegistrationForm.svelte";
import {
  clearRegistrationAccess,
  readRegistrationAccess,
  saveRegistrationAccess,
} from "#lib/registration-access.ts";

type RestoreState =
  | { status: "loading" }
  | { status: "ready" }
  | { status: "error"; message: string };

let registration = $state<Registration | null>(null);
let submitting = $state(false);
let registrationError = $state("");

let event = $state<EventDetails | null>(null);
let eventLoading = $state(true);
let eventRefreshing = $state(false);
let eventError = $state("");

let restoreState = $state<RestoreState>({ status: "loading" });
let storageWarning = $state("");

onMount(() => {
  void loadEvent();
});

onMount(() => {
  void restoreRegistration();
});

async function loadEvent() {
  if (eventRefreshing) {
    return;
  }
  eventLoading = event === null;
  eventRefreshing = event !== null;
  eventError = "";

  try {
    event = await getEvent();
  } catch (error) {
    console.error("Не удалось загрузить мероприятие", error);
    eventError =
      event === null
        ? "Не удалось загрузить мероприятие. Попробуйте позже."
        : "Не удалось обновить количество мест. Показаны последние полученные данные.";
  } finally {
    eventLoading = false;
    eventRefreshing = false;
  }
}

async function register(request: RegistrationRequest) {
  if (!event || submitting || registration) {
    return;
  }

  const normalizedName = request.name.trim();
  const normalizedEmail = request.email.trim();

  if (!normalizedEmail || !normalizedName) {
    registrationError = "Укажите имя и email";
    return;
  }

  submitting = true;
  registrationError = "";

  try {
    const result = await createRegistration({
      name: normalizedName,
      email: normalizedEmail,
    });

    registration = result.registration;
    storageWarning = "";

    try {
      saveRegistrationAccess({
        id: result.registration.id,
        accessToken: result.accessToken,
      });
    } catch {
      storageWarning =
        "Регистрация создана, но браузер не сохранил ключ доступа. " +
        "После обновления страницы автоматическое восстановление не сработает.";
    }

    await loadEvent();
  } catch (error) {
    if (error instanceof HttpError && error.status === 409) {
      registrationError =
        "Регистрация отклонена: мест нет или этот email уже зарегистрирован.";
      await loadEvent();
      return;
    }

    if (error instanceof HttpError && error.status === 400) {
      registrationError = "Проверьте имя и email";
      return;
    }

    console.error("Ошибка регистрации:", error);
    registrationError =
      "Не удалось получить подтверждение от сервера. Статус заявки неизвестен.";
  } finally {
    submitting = false;
  }
}

async function restoreRegistration() {
  restoreState = { status: "loading" };

  try {
    const access = readRegistrationAccess();

    if (access === null) {
      restoreState = { status: "ready" };
      return;
    }

    try {
      registration = await getRegistration(access.id, access.accessToken);
    } catch (error) {
      if (error instanceof HttpError && error.status === 404) {
        clearRegistrationAccess();
        registration = null;
        storageWarning =
          "Сохранённая заявка недоступна: она отсутствует или ключ больше не подходит.";
        restoreState = { status: "ready" };
        return;
      }

      throw error;
    }

    restoreState = { status: "ready" };
  } catch {
    restoreState = {
      status: "error",
      message:
        "Не удалось восстановить регистрацию. Сохраненный ключ не удален.",
    };
  }
}
</script>

<svelte:head>
  <title>{event ? `${event.title} – EventLab` : "EventLab"}</title>
</svelte:head>

<main>
  {#if eventLoading}
    <p class="notice" role="status">Загружаем мероприятие...</p>
  {:else if eventError && !event}
    <p class="notice notice--error" role="alert">{eventError}</p>
    <button class="button button--secondary" type="button" onclick={loadEvent}>
      Попробовать снова
    </button>
  {:else if event}
    <p class="eyebrow">Открытая лекция и мастер-класс</p>
    <h1>{event.title}</h1>

    <div class="description">
      <p>{event.description}</p>
    </div>

    <section class="details card" aria-label="Информация для участников">
      <div>
        <h2>Когда и где</h2>
        <p>{event.dateLabel}, {event.timeLabel}.</p>
        <p>{event.venue}.</p>
      </div>

      <div>
        <h2>Условия участия</h2>
        <p>Всего мест: {event.capacity}.</p>
        <p aria-live="polite">Свободных мест: {event.availableSeats}.</p>
      </div>
    </section>

    {#if eventRefreshing}
      <p class="notice" role="status">Обновляем количество мест...</p>
    {:else if eventError}
      <p class="notice notice--warning" role="alert">{eventError}</p>
      <button
        class="button button--secondary"
        type="button"
        onclick={loadEvent}
      >
        Обновить количество мест
      </button>
    {/if}
    <p class="storage-note">
      Заявка хранится на сервере. Ключ доступа сохраняется в этом браузере.
    </p>

    {#if storageWarning}
      <p class="notice notice--warning" role="status">{storageWarning}</p>
    {/if}

    {#if restoreState.status === "loading"}
      <p class="notice" role="status">Проверяем вашу регистрацию...</p>
    {:else if restoreState.status === "error"}
      <p class="notice notice--error" role="alert">{restoreState.message}</p>

      <button
        class="button button--secondary"
        type="button"
        onclick={restoreRegistration}
      >
        Повторить проверку регистрации
      </button>
    {:else if registration}
      <RegistrationConfirmation {registration} />
    {:else}
      <RegistrationForm
        {submitting}
        {registrationError}
        onRegister={register}
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
