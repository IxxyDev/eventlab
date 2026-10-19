# Design

## Source of truth

Status: Draft. Date: 2026-10-18.
Scope: the event page and registration flow.

## Brand

A calm, clear, and friendly interface.
A light background, green accents, and minimal decoration.
Avoid unnecessary gradients, shadows, and animations.

## Product goals

Help visitors understand participation details and register.
Keep registration confirmation visible if the seat count fails to refresh.
Do not add new product features at this stage.

## Personas and jobs

Visitors on mobile or desktop:
explore the event, register, and check their registration.

## Information architecture

Title and description → date and venue → participation details
→ registration form or confirmation.

## Design principles

Provide one primary call to action in each state.
Place related elements closer together.
Show errors near the affected operation.
Use shared styling rules for recurring elements.

## Visual language

Typography: system font, 1rem body text, and 1.6 line height.
Headings: responsive sizes and a simple hierarchy.
Colors: background #f5f5f2, surface #ffffff, text #202622.
Primary action: #285c3a with #ffffff text.
Focus: #326fc1.
Spacing scale: 4, 8, 12, 16, 24, 32, 48, and 64px.
Implement values using rem units and CSS custom properties.
Corner radii: 8px for controls and 16px for cards.
Decorative imagery and animations are not needed yet.

## Components

RegistrationForm: fields, user input, and submission of entered data.
RegistrationConfirmation: presentation of a confirmed registration.
Page: requests, registration recovery, and flow orchestration.
Shared design values belong in global CSS.
Native buttons share the global `.button` class for primary actions.
Retry actions use `.button.button--secondary` with a neutral surface and border.
Both variants define hover, active, keyboard focus, and disabled states.
Hover styles apply only when the device supports hovering.
Global `.card` styles define shared surfaces for details, forms, and confirmation.
Global `.notice` styles distinguish progress, errors, and warnings with text
and color. Confirmation uses a success surface and an explicit heading.
The registration form has a visible heading and an accessible name.

## Accessibility

Target: WCAG 2.2 AA; conformance requires verification.
Text and background colors must have a contrast ratio of at least 4.5:1.
Provide visible focus and keyboard access to every action.
Give fields visible labels.
Explain errors with text, not color alone.
Target an interactive element size of at least 44px.
Inputs and buttons have a minimum height of 2.75rem (44px at a 16px root size).
Respect the reduced motion preference.

## Responsive behavior

Start with narrow screens and preserve content order.
Avoid horizontal scrolling at a viewport width of 320 CSS pixels.
Limit content width and provide horizontal padding.
Keep actions accessible when text is enlarged.
Use border-box sizing so declared widths include padding and borders.
The page has a maximum outer width of 50rem, including 1rem side padding.
Page vertical padding scales from 2rem to 3rem with the viewport width.
Allow long email addresses and registration identifiers to wrap.
Event details use one column on narrow screens and two from 40rem.
Cards use responsive padding between 1rem and 1.5rem.

## Interaction states

Account for loading, refreshing, errors, and retries.
Prevent duplicate form submissions while a request is in progress.
Preserve entered values after a rejected submission.
Explain when displayed data may be stale.
Keep registration confirmation independent of seat count refreshes.

## Content voice

Use Russian for interface copy, with short and specific messages.
Explain what happened and which action is available.
Do not expose internal HTTP codes or exceptions to users.

## Implementation constraints

Svelte 5, TypeScript, and plain CSS.
No new UI libraries at this stage.

## Open questions

- [ ] Is a dark theme needed? Owner: the user.
  Design the light theme for now.
- [ ] Are event photographs needed? Owner: the user.
  Build the composition around text for now.
