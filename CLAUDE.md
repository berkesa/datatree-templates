# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

DataTree Templates is a small, fast server-side template engine for producing HTML, XML, XHTML, and plain-text output. It is Mustache-like but renders directly from hierarchical `Tree` structures (from the `datatree-core` dependency). The public Maven artifact is `com.github.berkesa:datatree-templates`; only the `io.datatree.templates` package is part of the published library.

## Build & Test

Maven project (`pom.xml`); compiled with `javac` targeting **Java 11** (`<maven.compiler.release>11</maven.compiler.release>`). Minimum consumer runtime: **JDK 11**. Build JDK: 17+ (JDK 25 in use). Build with `mvn`.

```bash
mvn clean verify           # compile + run unit tests (the definition-of-done gate)
mvn clean install          # also installs 2.0.0 to the local ~/.m2 repo
mvn test                   # run all unit tests
mvn -Prelease verify       # additionally builds sources + javadoc jars and GPG-signs (publishing)
```

Run a single test class or method:

```bash
mvn test -Dtest=TemplateEngineTest
mvn test -Dtest=TemplateEngineTest#testBase
```

Notes:
- Compilation uses plain `javac` via `maven-compiler-plugin`.
- **`PerformanceTest` is excluded** from the normal test run (surefire `<excludes>**/PerformanceTest*</excludes>`). It is a manual JMH-style benchmark with a `main()` (DataTree vs. FreeMarker/Mustache/Thymeleaf/Pebble), not a unit test — run it by hand if needed.
- Javadoc (under the `release` profile) excludes the `io.datatree.templates.html` fixtures package and `PojoTest`, with `<doclint>none</doclint>`.

### Dependency scope

The only **runtime/`compile`** dependency of the published library is `com.github.berkesa:datatree-core` (pinned to `2.0.0`). The comparison template engines (FreeMarker, Mustache, Thymeleaf, Pebble) and OpenPojo are **`test`-scoped** — they exist only to benchmark/validate and are *not* transitive dependencies of consumers.

### Source layout quirk

Template fixtures used by tests (e.g. `*.html`, `*.datatree`, `*.freemarker`, `*.mustache`, `*.pebble`, `*.thymeleaf`) live **next to the test classes** under `src/test/java/io/datatree/templates/html/` and are loaded from the classpath via `getClass().getResource(...)`. The `pom.xml` maps them as **test resources** (`<testResources>` over `src/test/java`, excluding `**/*.java`) so they are on the test classpath but **do not ship in the published jar**.

## Architecture

The engine is a two-phase compile-then-render pipeline. A template is parsed once into an immutable fragment tree, cached, then walked repeatedly against different data.

- **`TemplateEngine`** — the public entry point and runtime. Holds the `Cache<String, Fragment>` of compiled templates, the active `ResourceLoader`, config flags, and user functions. `process(path, data)` resolves the absolute path, fetches/compiles the template, and runs `transform(...)`. `transform` is a recursive `switch` over `Fragment.type` (the rendering interpreter). `define(path, source)` injects a template directly from a string instead of loading from disk.

- **`FragmentBuilder`** — the compiler. `compile(...)` scans the source for `#{...}` tags and recursively builds a `Fragment` tree. Block tags (`for`, `exists`/`ex`, `!exists`/`!ex`, `equals`/`eq`, `!equals`/`!eq`) recurse until a matching `#{end}`; the recursion returns the consumed character count so the parent can resume scanning.

- **`Fragment`** — a node in the compiled template tree: a `type` byte, two string slots (`arg`, `content`), an optional `function`, and a `children` array. For the cached root fragment, `content` doubles as a stringified `lastModified` timestamp used for reload detection.

- **`FragmentTypes`** — the `byte` type constants shared between the compiler and interpreter (`STATIC_TEXT`, `INSERTABLE_VARIABLE`, `FOR_CYCLE`, `CONDITION_TAG_*`, `INSERTABLE_TEMPLATE_FILE`, `FUNCTION`).

- **`ResourceLoader` / `DefaultLoader`** — pluggable template source. `DefaultLoader` reads from the filesystem first, then falls back to classpath resources. Swap via `setLoader(...)` to load from a DB, JAR, etc.

- **`SimpleHtmlMinifier`** — an optional `Function<String,String>` wired in via `setTemplatePreProcessor(...)`; runs once per template at load time (collapses whitespace, preserves `<script>` and `<pre>` content).

### Key behaviors to preserve

- **Caching & reload**: templates compile once and are cached by path. `reloadTemplates` should stay `false` in production; when `true`, `getTemplate` compares the loader's `lastModified` against the cached timestamp and recompiles on change.
- **HTML escaping**: when `escapeSpecialCharacters` is `true` (default), inserted variable values are XML-escaped by `writeXMLContent`. Static template text is never escaped.
- **Path resolution**: `getAbsolutePath` handles `rootDirectory` prefixing and relative includes (`../parts/header.html`); paths are normalized to `/` separators. Be careful editing this — it underpins `#{include}` / `#{in}`.
- **Custom functions**: registered via `addFunction(name, BiConsumer<StringBuilder, Tree>)` and invoked from templates with `#{fn name var}` / `#{function name var}`. Unknown function names throw at **compile** time.
- **Thread safety**: render buffers are pooled per-thread via a `ThreadLocal<StringBuilder>`; a single `TemplateEngine` is intended to be shared across threads.

### Template syntax (rendered by `transform`)

`#{var}` insert (supports dotted paths like `user.address.city`) · `#{include path}` / `#{in path}` sub-template · `#{for item : list}…#{end}` (colon optional) · `#{exists v}…#{end}` / `#{!exists v}…#{end}` · `#{equals v x}…#{end}` / `#{!equals v x}…#{end}` · `#{fn name v}` custom function. The `TemplateEngine` class javadoc is the authoritative syntax reference.

## Tests

`TemplateEngineTest` (JUnit 5 / Jupiter) is the main functional suite and exercises every tag against fixture templates. `PerformanceTest` benchmarks this engine against FreeMarker, Mustache, Thymeleaf, and Pebble — those engines are test-scope/comparison dependencies, **not** runtime dependencies of the library; it has a `main()` and is excluded from the surefire run. `PojoTest` (OpenPojo, JUnit 5) validates bean conventions and is excluded from javadoc.
