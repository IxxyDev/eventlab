<script lang="ts">
import favicon from "#lib/assets/favicon.svg";
import "#lib/styles/global.css";
import type { LayoutProps } from "./$types";

let { children }: LayoutProps = $props();
</script>

<svelte:head>
  <link rel="icon" href={favicon}>
</svelte:head>

<main>
  {@render children()}
</main>

<style>
main {
  max-width: 50rem;
  margin-inline: auto;
  padding: clamp(var(--space-8), 5vw, var(--space-12)) var(--space-4);
}
</style>
