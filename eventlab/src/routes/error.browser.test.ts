import { beforeEach, describe, expect, it, vi } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import { refreshAll } from "$app/navigation";
import * as appState from "$app/state";
import ErrorPage from "./+error.svelte";

vi.mock("$app/navigation", () => ({ refreshAll: vi.fn() }));
vi.mock("$app/state", () => ({ page: {} }));

describe("Error page", () => {
  beforeEach(() => {
    vi.mocked(refreshAll).mockReset();
  });

  it("should say that the page is not found", async () => {
    renderComponent(404);

    await expect
      .element(page.getByRole("heading", { name: "Страница не найдена" }))
      .toBeVisible();
  });

  it("should load the page again on retry", async () => {
    renderComponent(500);

    await page.getByRole("button", { name: "Попробовать снова" }).click();

    expect(vi.mocked(refreshAll).mock.calls).toEqual([[]]);
  });

  it("should lock the retry button while retrying", async () => {
    vi.mocked(refreshAll).mockReturnValue(new Promise(() => {}));
    renderComponent(500);

    await page.getByRole("button", { name: "Попробовать снова" }).click();

    await expect
      .element(page.getByRole("button", { name: "Пробуем снова…" }))
      .toBeDisabled();
  });

  it("should say that loading failed again after a retry", async () => {
    renderComponent(500);

    await page.getByRole("button", { name: "Попробовать снова" }).click();

    await expect
      .element(
        page.getByText(
          "Снова не удалось загрузить страницу. Попробуйте чуть позже.",
        ),
      )
      .toBeVisible();
  });

  function renderComponent(status: number) {
    appState.page.status = status;
    render(ErrorPage);
  }
});
