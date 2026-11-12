# Functional Kotlin Services

A 43-slide draft for a 50-minute Lambda World talk, with the full slot available for talk content. Built with Slidev and the local [Kotlin theme](../slidedev-theme-kotlin).

The draft builds from `suspend` and compositional resource lifetimes to context parameters, Ktor routes, domain types, and typed errors. The final service brings those capabilities together in direct-style Kotlin.

## Run the deck

Use Node.js 24 (`nvm use`) and install the dependencies:

```sh
npm ci
npm run dev
```

`npm run present:dev` enables remote control on the development server. `npm run build` produces the static site in `dist/`. `npm run export` exports a PDF; `npm run screenshot` exports PNG slides.

The theme is a local dependency at `../slidedev-theme-kotlin`; keep the sibling checkout available.

Use the committed lockfile: it matches the working reference decks' dependency versions, including Vite 8.3.0. This avoids a production CSS build failure observed with a fresh dependency resolution.

## Production presentation with a phone remote

```sh
npm run build
npm run present
```

The theme's `slidev-kotlin-present` serves the built `dist/` on port 4174
(`PORT=4175 npm run present` changes it) and prints the display link for the
laptop plus a QR code for the phone. Scan it on the same network; no internet
connection is required. Tap the right half to advance or the left half to go
back. The phone shows upcoming content, speaker notes, and connection status.

Only a page opened from the printed link can move the slides. Present over a
hotspot or a password-protected network: the link travels over plain HTTP.
After a rebuild, reload both devices. See "Presenting with a phone remote" in
the theme's README.

## Check the Kotlin examples

### Suspend gutter icons

Mark a suspension point with a `[!suspend]` code mark on its line; the theme
collects the marks of a fence into a `SuspendGutter` around it:

````md
```kotlin
suspend fun program() {
  kotlinx.coroutines.delay(100) // [!suspend]
  kotlinx.coroutines.yield() // [!suspend]
}
```
````

The marks identify manually selected potential suspension points; the component
does not analyze Kotlin. They share a comment with the other marks of the line
(`resourceScope { // [!context ResourceScope] [!suspend]`), move with their line
through a Magic Move chain, and never reach the generated Kotlin sources or the
handout. `SuspendGutter` ships with the theme since 0.14.0, together with the
`[!this …]`, `[!context …]`, `[!smartcast …]`, and `[!error …]` marks the
lessons use instead of hand-closed `TypeHint`, `SmartCast`, and
`InlineCompilerError` wrappers; see "Code marks" in the theme README.

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
