<script lang="ts">
import EventTimeAndPlace from "#lib/features/event/components/EventTimeAndPlace.svelte";
import { resolve } from "$app/paths";
import type { PageProps } from "./$types";

const { data }: PageProps = $props();
</script>

<svelte:head>
  <title>Мероприятия – EventLab</title>
</svelte:head>

<h1>Мероприятия</h1>

{#if data.events.length === 0}
  <p class="notice">Мероприятий пока нет.</p>
{:else}
  <ul>
    {#each data.events as event (event.id)}
      <li class="card">
        <h2>
          <a href={resolve("/events/[id]", { id: event.id })}>{event.title}</a>
        </h2>
        <EventTimeAndPlace {event} />
      </li>
    {/each}
  </ul>
{/if}

<style>
ul {
  display: grid;
  gap: var(--space-4);
  padding: 0;
  list-style: none;
}

h2 {
  margin-block: 0 var(--space-2);
  font-size: 1.25rem;
}

p {
  margin-block: var(--space-1);
}
</style>
