<script lang="ts">
import { formatEventSchedule } from "../event-schedule.ts";
import type { EventDetails } from "../types.ts";

interface Props {
  event: Pick<EventDetails, "startsAt" | "endsAt" | "timeZone" | "venue">;
}

const { event }: Props = $props();
</script>

<p class="time">
  <time datetime={event.startsAt}>{formatEventSchedule(event)}</time>
  по местному времени.
</p>
<p class="venue">{event.venue}.</p>

<style>
.time,
.venue {
  margin-block: var(--space-2);
}
</style>
