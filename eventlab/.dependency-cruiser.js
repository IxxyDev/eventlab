/** @type {import("dependency-cruiser").IConfiguration} */
export default {
  forbidden: [
    {
      name: "no-cross-feature",
      severity: "error",
      from: { path: "^src/lib/features/([^/]+)/" },
      to: {
        path: "^src/lib/features/([^/]+)/",
        pathNot: "^src/lib/features/$1/",
      },
    },
    {
      name: "shared-knows-no-features",
      severity: "error",
      from: { path: "^src/lib/shared/" },
      to: { path: "^src/lib/features/" },
    },
    {
      name: "lib-knows-no-routes",
      severity: "error",
      from: { path: "^src/lib/" },
      to: { path: "^src/routes/" },
    },
    {
      name: "relative-inside-feature",
      severity: "error",
      from: { path: "^src/lib/features/([^/]+)/" },
      to: { path: "^src/lib/features/$1/", dependencyTypes: ["aliased"] },
    },
    {
      name: "lib-alias-outside-feature",
      severity: "error",
      from: { path: "^src/lib/features/([^/]+)/" },
      to: {
        path: "^src/",
        pathNot: "^src/lib/features/$1/",
        dependencyTypesNot: ["aliased"],
      },
    },
    {
      name: "lib-alias-from-routes",
      severity: "error",
      from: { path: "^src/routes/" },
      to: { path: "^src/lib/", dependencyTypesNot: ["aliased"] },
    },
    {
      name: "no-circular",
      severity: "error",
      from: {},
      to: { circular: true },
    },
  ],
  options: {
    doNotFollow: { path: "node_modules" },
    tsPreCompilationDeps: true,
    tsConfig: { fileName: "tsconfig.json" },
  },
};
