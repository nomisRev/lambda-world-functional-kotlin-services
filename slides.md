---
theme: kotlin
title: Functional Kotlin Services
favicon: /kotlin.svg
transition: fade
layout: cover
class: fundamentals-cover
canvasWidth: 1440
aspectRatio: 16/9
colorSchema: both
highlighter: shiki
duration: 50min
timer: countdown
info: |
  Functional Kotlin Services at Lambda World. Suspension, structured concurrency,
  resources, context parameters, and typed errors in one service.
themeConfig:
  kodee: greeting
  drawnAnnotation:
    connect: false
  snippets:
    dir: src/main/kotlin/presentation/snippets
    package: presentation.snippets
    imports:
      - presentation.support.*
kodee: welcome
---

<!-- @formatter:off -->

# Functional Kotlin Services

## Simon Vergauwen · JetBrains

<!--

The audience already knows functional programming. Start from what we can build,
not a language contest. The examples follow one service from application lifetime
through HTTP into domain logic.
-->

---
layout: intro
class: section-slide agenda-slide
kodee: wave
---

# What?

1. Suspend and compose work
2. Ask for capabilities
3. Model the service
4. Make errors part of the contract

<!--

For orientation only: Scala's IO and ZIO, and Haskell's IO, are familiar places to
think about effectful programs. Today we will follow Kotlin's suspend functions.
Do not claim suspend is a pure effect type: it tracks suspension, and an ordinary
Kotlin function can still perform effects. We will build the useful capabilities.
-->

---
src: ./lessons/01-suspend.md
---
---
src: ./lessons/02-context.md
---
---
src: ./lessons/03-types.md
---
---
src: ./lessons/04-errors.md
---
---
src: ./lessons/closing.md
---
