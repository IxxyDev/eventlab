<script lang="ts">
import type { Registration } from "#lib/api/types.ts";

type Props = {
  registration: Registration;
};

let { registration }: Props = $props();
</script>

<div class="confirmation card" role="status">
  <h2>Вы зарегистрированы</h2>
  <p>{registration.name}, сервер подтвердил вашу регистрацию.</p>
  <p>Email: {registration.email}</p>
  <p>Номер заявки: {registration.id}</p>
</div>

<style>
.confirmation {
  margin-top: var(--space-8);
  border-color: var(--color-success-border);
  background: var(--color-success-background);
  color: var(--color-success);
}

h2 {
  margin: 0 0 var(--space-4);
  font-size: 1.5rem;
  line-height: 1.3;
}

p {
  margin-block: var(--space-2);
}
</style>
