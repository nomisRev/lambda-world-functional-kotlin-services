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

## Simon Vergauwen · Arrow-kt · JetBrains

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

1. Suspend and composing lifecycle
2. Constraining functions to a context
3. Domain modelling
4. Errors as part of the contract


---
src: ./lessons/01-suspend.md
---
---
src: ./lessons/closing.md
---
