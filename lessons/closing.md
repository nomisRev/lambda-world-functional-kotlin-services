---
layout: default
kodee: wave
---

# `suspend`, `context`, and typed errors

- `suspend` → asynchronous composition
- `CoroutineScope` and `ResourceScope` → owned lifetimes
- `context` → required capabilities
- Domain types and `Raise` → values and expected failures

> **Dependencies and errors are part of the function signature.**

<!--

We started with a function, then composed asynchronous work, cancellation,
resources, authentication, data, and errors. The business program asks for what
it needs; the boundary supplies and interprets it.
-->

---
layout: intro
class: section-slide
kodee: heart
---

<div class="lesson-number">Lambda World</div>

# Thank you
## Questions?

<!--
Useful follow-ups if questions arise: where cancellation stops at an SDK
boundary; why Raise differs from accumulating validation; how context resolution
works; what is stable versus preview versus the rich-errors proposal.
-->
