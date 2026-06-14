# TODO — Modernize `datatree-templates` to 2.0.0

> **You are the per-project Claude Code instance for `datatree-templates`.** Self-contained file.
> Goal: Gradle/Java 8 → **Maven + JDK 21**, deps upgraded, tests green on **JUnit 5**, legacy files
> removed, version **2.0.0**. This is a small Mustache-like server-side template engine that renders
> from `Tree`; only the `io.datatree.templates` package is the published library.

## Coordinates & facts
- Maven: `com.github.berkesa:datatree-templates`, `jar`, license **Apache-2.0**.
- `name`: *DataTree Templates* · `inceptionYear`: 2019
- `url`: https://berkesa.github.io/datatree-templates/ · `scm`: https://github.com/berkesa/datatree-templates.git
- developer: `berkesa` / Andras Berkes / andras.berkes@programmer.net
- **Version → `2.0.0`**.

## Inter-project dependency (PIN to 2.0.0)
- `com.github.berkesa:datatree-core:2.0.0` (was **1.0.10** — badly out of date). Build `datatree`
  first.

## ⚠ Two structural fixes (most important part)
1. **The comparison template engines are benchmark-only, but were declared `compile`** (FreeMarker,
   Jade, Mustache, Thymeleaf, Pebble). They are used only by `PerformanceTest` to benchmark against
   this engine — they must **not** be runtime dependencies of the published library. → declare them
   **`<scope>test</scope>`** (and drop Jade entirely, see below). After this, the only `compile`
   dependency is `datatree-core`.
2. **Source-layout quirk.** The Gradle build set `main.resources.srcDirs = ["src/test/java"]`, which
   bundled the test fixtures (`*.html`, `*.datatree`, `*.freemarker`, …) under
   `src/test/java/io/datatree/templates/html/` — loaded at test time via
   `getClass().getResource(...)`. In Maven, make these **test resources** instead (they should not
   ship in the published jar):
   ```xml
   <build>
     <testResources>
       <testResource>
         <directory>src/test/java</directory>
         <excludes><exclude>**/*.java</exclude></excludes>
       </testResource>
     </testResources>
   </build>
   ```
   This keeps `TemplateEngineTest` able to load fixtures from the test classpath while keeping the
   published jar clean. (Behavioral note for release notes: the published jar no longer contains
   test fixtures — intended.)

## Target versions
| Dependency | Current | Target | Scope |
|---|---|---|---|
| `com.github.berkesa:datatree-core` | 1.0.10 | **2.0.0** | compile |
| `org.freemarker:freemarker` | 2.3.29 | **2.3.34** | **test** |
| `com.github.spullara.mustache.java:compiler` | 0.9.6 | **0.9.14** | **test** |
| `org.thymeleaf:thymeleaf` | 3.0.11 | **3.1.x** (⚠ jakarta) | **test** |
| `com.mitchellbosecke:pebble` → `io.pebbletemplates:pebble` | 2.4.0 | **3.x** (⚠ jakarta, groupId moved) | **test** |
| `de.neuland-bfi:jade4j` | 1.2.7 | **DROP** (dead) | — |
| `com.openpojo:openpojo` | 0.8.10 | **0.9.x** | test |
| `junit:junit` 4.12 | → `junit-jupiter` | **5.x** | test |
| Eclipse `ecj` 4.4.2 | — | **remove** | — |
| Java | 1.8 | **21** | — |

## Steps
1. **`pom.xml`** with metadata + `release=21` + the `testResources` block above. `compile` dep:
   `datatree-core:2.0.0-SNAPSHOT`. All template engines `test` scope. Build plugins: compiler
   3.14.0, surefire 3.5.3, (release profile) sources/javadoc/gpg + central-publishing 0.9.0.
2. **Remove ECJ** → javac. The old `javadoc` excluded `**/html/**` and `PojoTest.java`; replicate
   with `maven-javadoc-plugin` `<excludePackageNames>`/`<sourceFileExcludes>` and
   `<doclint>none</doclint>`.
3. **Drop Jade4j**: remove the dependency and any `PerformanceTest` code path that benchmarks Jade.
4. **Tests → JUnit 5.** `TemplateEngineTest` currently extends JUnit 3 `junit.framework.TestCase` —
   convert to Jupiter `@Test` + `Assertions.*`, `setUp` → `@BeforeEach`. `PojoTest` (OpenPojo) stays
   excluded from javadoc. `PerformanceTest` is a benchmark — keep it out of the normal test run
   (surefire `<excludes>**/PerformanceTest*</excludes>` or `@Disabled`).
   Preserve engine behavior: caching/reload, HTML escaping (`writeXMLContent`), `getAbsolutePath`
   include resolution, custom functions compile-time validation, per-thread `StringBuilder` pooling.
5. **Cleanup — delete:** `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/`,
   `.gradle/`, `.travis.yml`, `.codacy.yaml`, `.classpath`, `.project`, `.settings/`.
6. **VSCode + .gitignore** (library — no `launch.json`).
7. **Build & install:** `mvn clean install`, then `mvn clean verify`.
8. **Update `CLAUDE.md`** commands to Maven; note the `compile`→`test` scope fix and the resources
   change.

## Definition of done
- `mvn clean verify` green on JDK 21; only `compile` dep is `datatree-core:2.0.0`.
- Comparison engines are test-scoped; Jade dropped; fixtures are test resources.
- JUnit 5; legacy files gone; VSCode + .gitignore; version `2.0.0(-SNAPSHOT)`; publishing configured.
