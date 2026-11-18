<script lang="ts">
import { refreshAll } from "$app/navigation";
import { resolve } from "$app/paths";
import { page } from "$app/state";

const isNotFound = $derived(page.status === 404);
const title = $derived(
  isNotFound ? "Страница не найдена" : "Не удалось загрузить страницу",
);
</script>

<svelte:head>
  <title>{title} – EventLab</title>
</svelte:head>

<a href={resolve("/")}>Все мероприятия</a>

<h1>{title}</h1>

{#if isNotFound}
  <p>Возможно, ссылка устарела или в ней опечатка.</p>
{:else}
  <button class="button" type="button" onclick={() => refreshAll()}>
    Попробовать снова
  </button>
{/if}
