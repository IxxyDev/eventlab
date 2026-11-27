<script lang="ts">
import EventTimeAndPlace from "#lib/features/event/components/EventTimeAndPlace.svelte";
import { resolve } from "$app/paths";
import type { PageProps } from "./$types";

const { data }: PageProps = $props();
</script>

<svelte:head>
  <title>Мероприятия – EventLab</title>
  <meta
    name="description"
    content="Найдите мероприятие, узнайте, когда и где оно пройдёт, и запишитесь на участие."
  >
</svelte:head>

<h1>Мероприятия</h1>

{#if data.events.length === 0}
  <p class="notice">Мероприятий пока нет.</p>
{:else}
  <ul class="list">
    {#each data.events as event (event.id)}
      <li class="card">
        <h2 class="title">
          <a href={resolve("/events/[id]", { id: event.id })}>{event.title}</a>
        </h2>
        <p class="organizer">Организатор: {event.organizationName}</p>
        <EventTimeAndPlace {event} />
      </li>
    {/each}
  </ul>
{/if}

<style>
.list {
  display: grid;
  gap: var(--space-4);
  padding: 0;
  list-style: none;
}

.organizer {
  margin-block: var(--space-2);
}

.title {
  margin-block: 0 var(--space-2);
  font-size: 1.25rem;
}
</style>
