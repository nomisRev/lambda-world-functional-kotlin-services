# Functional Kotlin Services

A 43-slide draft for a 50-minute Lambda World talk, with the full slot available for talk content. Built with Slidev and the local [Kotlin theme](../slidedev-theme-kotlin).

The draft builds from `suspend` and compositional resource lifetimes to context parameters, Ktor routes, domain types, and typed errors. The final service brings those capabilities together in direct-style Kotlin.

## Run the deck

Use Node.js 24 (`nvm use`) and install the dependencies:

```sh
npm ci
npm run dev
```

`npm run present` enables remote presentation control. `npm run build` produces the static site in `dist/`. `npm run export` exports a PDF; `npm run screenshot` exports PNG slides.

The theme is a local dependency at `../slidedev-theme-kotlin`; keep the sibling checkout available.

Use the committed lockfile: it matches the working reference decks' dependency versions, including Vite 8.3.0. This avoids a production CSS build failure observed with a fresh dependency resolution.

## Check the Kotlin examples

### Suspend gutter icons

Wrap a single code fence in the auto-imported `SuspendGutter` component:

````md
<SuspendGutter :lines="[2, 3]">

```kotlin
suspend fun program() {
  kotlinx.coroutines.delay(100)
  kotlinx.coroutines.yield()
}
```

</SuspendGutter>
````

`lines` contains one-based source line numbers within the fence, counting blank
lines regardless of any displayed `startLine`. Duplicate numbers are deduplicated;
invalid, blank, and missing lines are ignored. The markers identify manually
selected potential suspension points; the component does not analyze Kotlin.
An optional `label` prop changes the default “Suspension point” accessible label
and tooltip.

This deck-local component uses the sibling theme's line-matching helpers for
ordinary Shiki fences and Magic Move. It follows the current step's line numbers,
hides during token transitions, and places icons outside the code window's left
border without changing the window position or code padding. The supplied
JetBrains suspend-call SVGs switch automatically between light and dark variants.
Use `--suspend-gutter-size` (default `1em`) and `--suspend-gutter-gap` (default
`0.35em`, between the icon and window) in CSS to customize their placement.

### Compilation

The theme extracts compilable Kotlin fences from the slides into `src/main/kotlin/presentation/snippets`. Shared example types live in `src/main/kotlin/presentation/support`.

The checked examples use Kotlin 2.4.20, JVM 21, Ktor 3.6.0, KotlinX Coroutines 1.11.0, and Arrow / SuspendApp 2.2.3, with the Gradle 9.7.1 wrapper. Context-sensitive resolution and name-based destructuring are enabled explicitly in `build.gradle.kts`; Ktor typed authentication and Arrow accumulation APIs have their required opt-ins.

After editing a Kotlin fence:

```sh
npm run snippets
npm run snippets:check
./gradlew check
```

`snippets:check` verifies generated sources match the slides. Gradle also runs this freshness check, compiles 33 generated sources, and runs 13 tests. Tests exercise SDK cancellation, resource release and cancellation, receiver/context resolution, validation accumulation, and the composed greeting service. `npm run check` runs the freshness check, Gradle checks, and the Slidev production build together.

Fences explicitly tagged `no-compile`, and examples inside `InlineCompilerError`, are excluded from compilation. Proposed language syntax must remain visibly identified as a proposal. Successful checks cover the executable examples, not proposals or a complete deployed application.

The two `no-compile` fences show the Ktor `principal` property signature without its implementation and the rich-error `|.` proposal. The working validation examples use the application-level `validate` helper from `ktor-arrow-example`, built on Arrow's `RaiseAccumulate` to collect errors both within and across fields. Value classes compile today; the JVM Valhalla discussion describes future runtime work.

## Talk outline

| Section | Content |
| --- | --- |
| Opening | Functional Kotlin Services |
| Coroutines | `suspend`, cancellation, concurrency, streams, resources |
| Context | Dependencies, typed authentication, routes, delegation |
| Domain types | Nullability, value classes, destructuring, sealed interfaces |
| Typed errors | `Raise`, field validation, rich-error proposal, service composition |

The 50-minute duration is the target for the completed talk, not a measured runtime of this draft. There is no reserved Q&A block. Per-slide time estimates have been removed to reflect the presenter's fast pace. Discuss additional topics with Simon before extending the scope.

Speaker notes hold explanations and source references. Voice references are the October 8 revisions of `kotlin-devoxx-slides/slides.md` and `advanced-dsl-in-kotlin/lessons/`: small code changes, concrete headings, compiler feedback, and annotations at the relevant tokens.
