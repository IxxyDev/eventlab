<script lang="ts">
    import { onMount } from "svelte";

    let name = $state("");
    let email = $state("");
    let registration = $state<Registration | null>(null);
    let submitting = $state(false);
    let registrationError = $state("");
    let event = $state<EventDetails | null>(null);
    let eventLoading = $state(true);
    let eventError = $state("");

    async function loadEvent() {
        eventLoading = true;
        eventError = "";

        try {
            const response = await fetch("http://localhost:8080/api/event");

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            event = await response.json();
        } catch (error) {
            console.error("Не удалось загрузить мероприятие", error);
            eventError = "Не удалось загрузить мероприятие. Попробуйте позже.";
        } finally {
            eventLoading = false;
        }
    }

    onMount(() => {
        void loadEvent();
    });

    async function register(e: SubmitEvent) {
        e.preventDefault();

        if (!event || submitting || registration) {
            return;
        }

        const normalizedName = name.trim();
        const normalizedEmail = email.trim();

        if (!normalizedEmail || !normalizedName) {
            registrationError = "Укажите имя и email";
            return;
        }

        submitting = true;
        registrationError = "";

        try {
            const response = await fetch(
                "http://localhost:8080/api/event/registrations",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        name: normalizedName,
                        email: normalizedEmail,
                    }),
                },
            );

            if (response.status === 409) {
                registrationError =
                    "Регистрация отклонена: мест нет или этот email уже зарегистрирован.";
                await loadEvent();
                return;
            }

            if (response.status === 400) {
                registrationError = "Проверьте имя и email.";
                return;
            }

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            registration = await response.json();
            await loadEvent();
        } catch (error) {
            console.error("Ошибка регистрации:", error);
            registrationError =
                "Не удалось получить подтверждение от сервера. Статус заявки неизвестен.";
        } finally {
            submitting = false;
        }
    }

    type Registration = {
        id: string;
        name: string;
        email: string;
    };

    type EventDetails = {
        id: string;
        title: string;
        description: string;
        dateLabel: string;
        timeLabel: string;
        venue: string;
        capacity: number;
        availableSeats: number;
    };
</script>

<svelte:head>
    <title>{event ? `${event.title} – EventLab` : "EventLab"}</title>
</svelte:head>

<main>
    {#if eventLoading}
        <p role="status">Загружаем мероприятие...</p>
    {:else if eventError}
        <p role="alert">{eventError}</p>
        <button type="button" onclick={loadEvent}>Попробовать снова</button>
    {:else if event}
        <p class="eyebrow">Открытая лекция и мастер-класс</p>
        <h1>{event.title}</h1>

        <div class="description">
            <p>{event.description}</p>
        </div>

        <section class="details" aria-label="Информация для участников">
            <h2>Когда и где</h2>
            <p>{event.dateLabel}, {event.timeLabel}.</p>
            <p>{event.venue}.</p>

            <h2>Условия участия</h2>
            <p>Всего мест: {event.capacity}.</p>
            <p aria-live="polite">
              Свободных мест: {event.availableSeats}.
            </p>
        </section>

        <p>Демонстрация регистрации: сохраняется только в этом браузере.</p>

        {#if registration}
            <div role="status">
                <p>
                    {registration.name}, сервер подтвердил вашу регистрацию.
                </p>
                <p>Email: {registration.email}</p>
                <p>Номер заявки: {registration.id}</p>
            </div>
        {:else}
            <form onsubmit={register} aria-busy={submitting}>
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
                    />
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
                    />
                </label>

                {#if registrationError}
                    <p role="alert">{registrationError}</p>
                {/if}

                <button type="submit" disabled={submitting}>
                    {submitting ? "Отправляем заявку…" : "Записаться"}
                </button>
            </form>
        {/if}
    {/if}
</main>

<style>
    :global(body) {
        margin: 0;
        background: #f5f5f2;
        color: #202622;
        font-family: system-ui, sans-serif;
        line-height: 1.6;
    }

    main {
        max-width: 48rem;
        margin-inline: auto;
        padding: 3rem 1.25rem;
    }

    .eyebrow {
        color: #47644e;
        font-weight: 600;
    }

    h1 {
        margin-block: 1rem 1.5rem;
        font-size: clamp(2rem, 6vw, 3.5rem);
        line-height: 1.1;
    }

    .description {
        font-size: 1.125rem;
    }

    .details {
        margin-top: 2rem;
        padding: 1.5rem;
        background: white;
        border: 1px solid #dce2da;
        border-radius: 1rem;
    }

    h2 {
        margin-block: 1.5rem 0.5rem;
        font-size: 1.125rem;
    }

    h2:first-child {
        margin-top: 0;
    }

    p {
        margin-block: 0.5rem;
    }

    form {
        display: grid;
        gap: 1rem;
        margin-top: 1rem;
    }

    label {
        display: grid;
        gap: 0.375rem;
    }

    input {
        min-width: 0;
        padding: 0.75rem;
        border: 1px solid #89968b;
        border-radius: 0.5rem;
        font: inherit;
    }

    button {
        padding: 0.75rem 1rem;
        border: 0;
        border-radius: 0.5rem;
        background: #285c3a;
        color: white;
        font: inherit;
        cursor: pointer;
    }

    button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    input:focus-visible,
    button:focus-visible {
        outline: 3px solid #326fc1;
        outline-offset: 3px;
    }
</style>
