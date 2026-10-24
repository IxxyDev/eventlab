import { execFileSync } from "node:child_process";
import { join } from "node:path";

const databaseUrl =
  "postgresql://eventlab:eventlab@127.0.0.1:5433/eventlab_e2e";

export default function globalSetup() {
  execFileSync(
    "psql",
    [
      databaseUrl,
      "--set=ON_ERROR_STOP=1",
      `--file=${join(import.meta.dirname, "reset.sql")}`,
    ],
    { stdio: "inherit" },
  );
}
