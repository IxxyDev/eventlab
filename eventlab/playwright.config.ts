import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "e2e",
  globalSetup: "./e2e/global-setup.ts",
  reporter: [["list"], ["html", { open: "never" }]],
  workers: 1,
  forbidOnly: !!process.env.CI,
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: "./gradlew bootRun",
      cwd: "../eventlab-api",
      env: {
        SPRING_DATASOURCE_URL: "jdbc:postgresql://127.0.0.1:5433/eventlab_e2e",
      },
      url: "http://localhost:8080/api/event",
      reuseExistingServer: false,
      timeout: 180_000,
    },
    {
      command: "npm run dev -- --port 5173 --strictPort",
      url: "http://localhost:5173",
      reuseExistingServer: !process.env.CI,
    },
  ],
});
