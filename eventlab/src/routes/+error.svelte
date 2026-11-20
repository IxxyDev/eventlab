<script lang="ts">
import { refreshAll } from "$app/navigation";
import { resolve } from "$app/paths";
import { page } from "$app/state";

const isNotFound = $derived(page.status === 404);
const title = $derived(
  isNotFound ? "Страница не найдена" : "Не удалось загрузить страницу",
);

let retryStatus = $state<RetryStatus>("idle");

async function retryLoading() {
  retryStatus = "retrying";
  await refreshAll();
  // A successful load replaces this page, so still being here means it failed again.
  retryStatus = "failed";
}

type RetryStatus = "idle" | "retrying" | "failed";
</script>

<svelte:head>
  <title>{title} – EventLab</title>
</svelte:head>

<a href={resolve("/")}>Все мероприятия</a>

<h1>{title}</h1>

{#if isNotFound}
  <p>Возможно, ссылка устарела или в ней опечатка.</p>
{:else}
  {#if retryStatus === "failed"}
    <p class="notice notice--error" role="alert">
      Снова не удалось загрузить страницу. Попробуйте чуть позже.
    </p>
  {/if}

  <button
    class="button"
    type="button"
    disabled={retryStatus === "retrying"}
    onclick={retryLoading}
  >
    {retryStatus === "retrying" ? "Пробуем снова…" : "Попробовать снова"}
  </button>
{/if}
