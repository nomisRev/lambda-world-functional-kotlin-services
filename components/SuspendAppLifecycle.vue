<script setup lang="ts">
import { computed } from 'vue'
import { useNav, useSlideContext } from '@slidev/client'

// One deterministic timeline for presenter clicks, reverse navigation and export.
const { $clicks } = useSlideContext()
const nav = useNav()
const step = computed(() => Math.min(4, Math.max(0, $clicks.value)))
const stages = ['Running', 'Cancel', 'Join', 'Release', 'Exit']
const captions = [
  'Own the work. Own its lifetime.',
  'A shutdown signal becomes cooperative cancellation.',
  'Await children and their cleanup before releasing shared resources.',
  'Keep the process alive while the resource scope closes the database.',
  'Only after cleanup completes can shutdown finish.',
]
const work = computed(() => step.value === 0 ? 'running' : step.value === 1 ? 'cancelling · cleanup' : 'joined ✓')
</script>

<template>
  <section class="lifecycle" aria-label="SuspendApp shutdown lifecycle">
    <div class="stages" aria-label="Shutdown stages">
      <span v-for="(label, index) in stages" :key="label" :class="{ current: step === index, passed: step > index }">
        <b>{{ index + 1 }}</b> {{ label }}
      </span>
    </div>

    <div class="process" :class="{ waiting: step > 0 && step < 4, exited: step === 4 }">
      <header><span class="process-title">Process</span><span class="process-state">{{ step === 0 ? 'alive' : step < 4 ? 'alive · awaiting application cleanup' : 'shutdown complete ✓' }}</span></header>
      <div class="signal" :class="{ visible: step > 0 }">SIGTERM / SIGINT <span>↓</span></div>
      <div class="app node" :class="{ cancelling: step > 0 && step < 4, done: step === 4 }">
        <strong>SuspendApp</strong>
        <small>{{ step === 0 ? 'application scope' : step < 4 ? 'cancel → await completion' : 'application completed ✓' }}</small>
      </div>
      <svg class="branches" viewBox="0 0 1000 58" preserveAspectRatio="none" aria-hidden="true" :class="{ cancelling: step === 1, done: step >= 2 }">
        <path d="M500 0 V22 H250 V57 M500 22 H750 V57" />
        <path class="arrow" d="M244 49 L250 57 L256 49 M744 49 L750 57 L756 49" />
      </svg>
      <div class="workers">
        <div v-for="(name, index) in ['HTTP server', 'Event processor']" :key="name" class="node worker" :class="{ running: step === 0, cancelling: step === 1, done: step >= 2 }" :style="{ transitionDelay: `${index * 130}ms` }">
          <strong>{{ name }}</strong><small>{{ work }}</small>
        </div>
      </div>
      <div class="resource" :class="{ releasing: step === 3, done: step === 4 }">
        <span>Shared resource · database</span>
        <strong>{{ step < 3 ? 'open' : step === 3 ? 'closing…' : 'closed ✓' }}</strong>
      </div>
    </div>

    <footer>
      <p aria-live="polite">{{ captions[step] }}</p>
      <div class="controls">
        <button aria-label="Previous lifecycle stage" :disabled="step === 0" @click.stop="nav.prev()">←</button>
        <button aria-label="Next lifecycle stage" :disabled="step === 4" @click.stop="nav.next()">→</button>
      </div>
    </footer>
    <div class="qualification">Cooperative shutdown · within the configured grace period</div>
  </section>
</template>

<style scoped>
.lifecycle { --purple: #7954f6; --amber: #b66b06; --green: #24876a; color: var(--fundamentals-ink); margin-top: 1.6rem; }
.stages { display: flex; gap: 2.5rem; font-size: 1.15rem; margin-bottom: 1.3rem; }
.stages span { opacity: .42; transition: opacity 250ms; }
.stages .current { opacity: 1; color: var(--purple); }
.stages .passed { opacity: .85; }
.stages b { display: inline-grid; place-items: center; border: 1.5px solid currentColor; border-radius: 50%; width: 1.7rem; height: 1.7rem; margin-right: .4rem; }
.process { border: 2px solid var(--purple); border-radius: 1.15rem; padding: 1rem 2rem 1.25rem; transition: border-color 350ms; }
.process.waiting { border-color: var(--amber); }
.process.exited { border-color: var(--green); }
header { display: flex; justify-content: space-between; align-items: center; }
.process-title { font-weight: 700; font-size: 1.45rem; }
.process-state { font-family: var(--slidev-font-mono); font-size: 1.05rem; }
.waiting .process-state { color: var(--amber); }
.exited .process-state { color: var(--green); }
.signal { height: 2.5rem; font-family: var(--slidev-font-mono); font-size: 1.05rem; color: var(--amber); text-align: center; opacity: 0; transition: opacity 250ms; }
.signal.visible { opacity: 1; }
.signal span { margin-left: .6rem; font-size: 1.6rem; }
.node { border: 2px solid var(--purple); border-radius: .75rem; text-align: center; padding: .6rem 1rem; transition: border-color 350ms, background 350ms; }
.node strong { display: block; font-family: var(--slidev-font-mono); font-size: 1.4rem; }
.node small { display: block; font-size: 1.05rem; margin-top: .2rem; }
.app { width: 26rem; margin: auto; background: rgb(121 84 246 / 6%); }
.branches { width: 100%; height: 3rem; stroke: var(--purple); stroke-width: 2; fill: none; transition: stroke 350ms; }
.workers { display: grid; grid-template-columns: 1fr 1fr; gap: 3rem; }
.worker { margin-inline: 2rem; }
.cancelling { border-color: var(--amber); stroke: var(--amber); background: rgb(217 119 6 / 7%); }
.node.cancelling { border-style: dashed; }
.done { border-color: var(--green); stroke: var(--green); }
.resource { display: flex; justify-content: space-between; border-top: 1px solid rgb(121 84 246 / 25%); margin-top: 1.4rem; padding-top: .8rem; font-size: 1.15rem; }
.resource strong { color: var(--purple); }
.resource.releasing strong { color: var(--amber); }
.resource.done strong { color: var(--green); }
footer { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-top: 1rem; }
footer p { margin: 0; font-size: 1.35rem; font-weight: 650; }
.controls { display: flex; gap: .5rem; }
button { border: 1px solid var(--purple); border-radius: .4rem; color: var(--purple); padding: .1rem .8rem; font-size: 1.3rem; cursor: pointer; }
button:disabled { opacity: .25; cursor: default; }
button:focus-visible { outline: 3px solid var(--purple); outline-offset: 3px; }
.qualification { opacity: .6; font-size: .95rem; margin-top: .5rem; }
.running { animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 50% { box-shadow: 0 0 0 5px rgb(121 84 246 / 10%); } }
html.dark .lifecycle { --amber: #f0b65a; --green: #65c9a5; --purple: #aa8aff; }
@media (prefers-reduced-motion: reduce) { *, .running { animation: none; transition: none; } }
</style>
