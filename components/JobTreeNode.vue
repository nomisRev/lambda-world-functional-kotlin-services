<script setup lang="ts">
import { computed } from 'vue'
import { useSlideContext } from '@slidev/client'
import { broken, ordinal, stateOf, stagger, type JobKind, type JobNode, type State } from './jobTree'

const props = withDefaults(defineProps<{
  node: JobNode
  /** Position among its siblings, for the stagger. */
  index?: number
  /** This node is one entry of a scope's resource stack: its children hang off to the side. */
  stacked?: boolean
}>(), { index: 0, stacked: false })

const { $clicks } = useSlideContext()

const kind = computed(() => props.node.kind ?? 'job')
const state = computed(() => stateOf(props.node, $clicks.value))
const children = computed(() => props.node.children ?? [])
const childStates = computed(() => children.value.map(child => stateOf(child, $clicks.value)))

/**
 * A resource says what is happening to it. Its release is caused by whatever
 * ended the scope's block, so the `ExitCase` is named while it runs.
 */
const caption = computed(() => {
  if (kind.value !== 'resource') return ''
  switch (state.value) {
    case 'running': return props.node.order ? `acquired ${ordinal(props.node.order)}` : 'acquired'
    case 'finalising': return props.node.exit ? `release(ExitCase.${props.node.exit})` : 'releasing'
    case 'cancelling': return 'cancelling'
    case 'done': return 'released'
    case 'failed': return 'release(ExitCase.Failure)'
    default: return ''
  }
})

/**
 * A scope holds its resources as a stack: acquired in order, released in
 * reverse. They are drawn in acquisition order, as in the code, and the
 * clicks release them from the bottom up.
 */
const stack = computed(() => kind.value === 'scope' && children.value.length > 0)
const stackEntries = computed(() => children.value.map((child, index) => ({ child, index })))

/** The colour of the edge towards one child, from that child's state. */
const edgeClass = (childState: State, childKind: JobKind, both = true) => ({
  'is-failed': childState === 'failed' && (!both || broken(state.value)),
  'is-cancelled': (childState === 'cancelling' || childState === 'cancelled') && (!both || broken(state.value)),
  'is-finalising': childState === 'finalising',
  'is-released': childKind === 'resource' && childState === 'done',
})

/** One edge per child, from this node's bottom centre to the child's column centre. */
const edges = computed(() => children.value.map((child, index) => {
  const count = children.value.length
  const x = ((index + 0.5) / count) * 100
  return {
    d: `M50 0 L${x} 20`,
    visible: childStates.value[index] !== 'pending',
    // A scope is reached by a suspending call, not owned as a child Job.
    suspend: (child.kind ?? 'job') === 'scope',
  }
}))
</script>

<template>
  <div class="jobtree-branch" :class="{ 'is-stacked': stacked }">
    <div class="jobtree-row">
      <div
        class="jobtree-node"
        :class="[`is-${state}`, `kind-${kind}`]"
        :style="{ '--jobtree-stagger': stagger(index) }"
      >
        <span class="jobtree-label">{{ node.label }}</span>
        <small v-if="caption" class="jobtree-caption">{{ caption }}</small>
      </div>
      <template v-if="stacked && children.length">
        <svg class="jobtree-tie" viewBox="0 0 100 20" preserveAspectRatio="none" aria-hidden="true">
          <path
            v-for="(child, i) in children"
            :key="i"
            d="M0 10 H100"
            :class="{ 'is-visible': childStates[i] !== 'pending', ...edgeClass(childStates[i], child.kind ?? 'job', false) }"
          />
        </svg>
        <div class="jobtree-side">
          <JobTreeNode v-for="(child, i) in children" :key="i" :node="child" :index="i" />
        </div>
      </template>
    </div>

    <template v-if="!stacked && stack">
      <svg class="jobtree-fan" viewBox="0 0 100 20" preserveAspectRatio="none" aria-hidden="true">
        <path d="M50 0 V20" class="is-visible" :class="{ 'is-cancelled': broken(state) }" />
      </svg>
      <div class="jobtree-stack">
        <JobTreeNode
          v-for="{ child, index: i } in stackEntries"
          :key="i"
          :node="child"
          :index="i"
          stacked
        />
      </div>
    </template>

    <template v-else-if="!stacked && children.length">
      <svg class="jobtree-fan" viewBox="0 0 100 20" preserveAspectRatio="none" aria-hidden="true">
        <path
          v-for="(edge, i) in edges"
          :key="i"
          :d="edge.d"
          :class="{ 'is-visible': edge.visible, 'is-suspend': edge.suspend, ...edgeClass(childStates[i], children[i].kind ?? 'job') }"
          :style="{ transitionDelay: stagger(i) }"
        />
      </svg>
      <div class="jobtree-children" :style="{ '--jobtree-columns': children.length }">
        <JobTreeNode v-for="(child, i) in children" :key="i" :node="child" :index="i" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.jobtree-branch {
  display: grid;
  align-content: start;
  justify-items: center;
  width: 100%;
}
.jobtree-row {
  display: flex;
  align-items: center;
}

/* A scope's resources: one stack in acquisition order, released bottom-up. */
.jobtree-stack {
  display: grid;
  gap: 0.35rem;
  justify-items: start;
  padding: 0.5rem 0.6rem;
  border-radius: 0.7rem;
  background: rgb(121 84 246 / 7%);
}
.jobtree-branch.is-stacked { width: auto; justify-items: start; }
.jobtree-branch.is-stacked > .jobtree-row > .jobtree-node { min-width: 12.5rem; }

/* A stacked resource's own children sit beside their entry, tied by a short line. */
.jobtree-tie {
  flex: none;
  width: 2rem;
  height: 1rem;
  fill: none;
  stroke: var(--jobtree-running);
  stroke-width: 2.5;
}
.jobtree-tie path {
  vector-effect: non-scaling-stroke;
  opacity: 0;
  transition: opacity 250ms ease, stroke 250ms ease;
}
.jobtree-tie path.is-visible { opacity: 1; }
.jobtree-tie path.is-failed { stroke: var(--jobtree-failed); }
.jobtree-tie path.is-cancelled { stroke: var(--jobtree-cancelled); stroke-dasharray: 6 5; }
.jobtree-tie path.is-finalising { stroke: var(--jobtree-finalising); }
.jobtree-side { display: grid; justify-items: center; }

.jobtree-node {
  position: relative;
  display: grid;
  justify-items: center;
  padding: 0.55rem 1.2rem;
  border: 2.5px solid var(--jobtree-running);
  border-radius: 0.8rem;
  background: rgb(255 255 255 / 92%);
  color: var(--jobtree-ink);
  white-space: nowrap;
  transition: opacity 250ms ease, transform 250ms ease, border-color 250ms ease, color 250ms ease, background 250ms ease;
  transition-delay: var(--jobtree-stagger, 0ms);
}
/* The strike-through is always there, transparent until the node is cancelled,
   so it can fade in with the same stagger as the border. */
.jobtree-label {
  text-decoration: line-through;
  text-decoration-color: transparent;
  text-decoration-thickness: 0.12em;
  transition: text-decoration-color 250ms ease;
  transition-delay: var(--jobtree-stagger, 0ms);
}
.jobtree-caption {
  font-size: 0.72em;
  font-weight: 500;
  letter-spacing: 0;
  opacity: 0.85;
}

/* A scope is a call on the stack, not a Job: thinner, dotted. */
.jobtree-node.kind-scope { border-width: 2px; border-style: dotted; }
/* A resource is a thing that is held: square, with a caption for its state. */
.jobtree-node.kind-resource { border-radius: 0.25rem; }

.jobtree-node.is-done::after,
.jobtree-node.is-failed::after,
.jobtree-node.is-cancelled::after {
  position: absolute;
  top: -0.9rem;
  right: -0.9rem;
  display: grid;
  place-items: center;
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  color: #fff;
  font-size: 1.2rem;
  line-height: 1;
  animation: jobtree-badge 200ms ease both;
  animation-delay: var(--jobtree-stagger, 0ms);
}

.jobtree-node.is-pending {
  opacity: 0;
  transform: translateY(-0.5rem);
}
.jobtree-node.is-running {
  animation: jobtree-pulse 1.6s ease-in-out infinite;
}
.jobtree-node.is-done {
  border-color: var(--jobtree-done);
  background: rgb(47 163 127 / 14%);
  color: #176b52;
}
.jobtree-node.is-done::after {
  background: var(--jobtree-done);
  content: '✓';
}
/* Clean-up in progress: a resource's release, a `finally` block. Blue, so it
   is neither the amber of cancellation nor the green of success. */
.jobtree-node.is-finalising {
  border-color: var(--jobtree-finalising);
  background: rgb(14 127 179 / 14%);
  color: #0a5d84;
  animation: jobtree-finalise-pulse 1.1s ease-in-out infinite;
  animation-delay: var(--jobtree-stagger, 0ms);
}
/* A released resource is gone: the finalising blue, dimmed, no badge. */
.jobtree-node.kind-resource.is-done {
  border-color: var(--jobtree-finalising);
  background: transparent;
  color: var(--jobtree-finalising);
  opacity: 0.55;
}
.jobtree-node.kind-resource.is-done::after { content: none; }
.jobtree-node.is-failed {
  border-color: var(--jobtree-failed);
  background: rgb(211 50 90 / 12%);
  color: #a11d45;
}
.jobtree-node.is-failed::after {
  background: var(--jobtree-failed);
  content: '!';
}
.jobtree-node.is-cancelling {
  border-style: dashed;
  border-color: var(--jobtree-cancelled);
  background: rgb(217 119 6 / 16%);
  color: #9a5b00;
  animation: jobtree-cancel-pulse 1.1s ease-in-out infinite;
  animation-delay: var(--jobtree-stagger, 0ms);
}
.jobtree-node.is-cancelled {
  border-style: dashed;
  border-color: var(--jobtree-cancelled);
  background: rgb(217 119 6 / 22%);
  color: #767982;
  opacity: 0.6;
  transform: scale(0.94);
}
.jobtree-node.is-cancelled .jobtree-label {
  text-decoration-color: var(--jobtree-cancelled);
}
.jobtree-node.is-cancelled::after {
  background: var(--jobtree-cancelled);
  content: '✕';
}

.jobtree-fan {
  width: 100%;
  height: 3rem;
  fill: none;
  stroke: var(--jobtree-running);
  stroke-width: 2.5;
}
.jobtree-fan path {
  vector-effect: non-scaling-stroke;
  opacity: 0;
  transition: opacity 250ms ease, stroke 250ms ease;
}
.jobtree-fan path.is-visible { opacity: 1; }
.jobtree-fan path.is-suspend { stroke-dasharray: 2 5; stroke-linecap: round; }
.jobtree-fan path.is-failed { stroke: var(--jobtree-failed); }
.jobtree-fan path.is-cancelled { stroke: var(--jobtree-cancelled); stroke-dasharray: 6 5; }
.jobtree-fan path.is-finalising { stroke: var(--jobtree-finalising); }
.jobtree-fan path.is-released { stroke: var(--jobtree-finalising); opacity: 0.55; }

.jobtree-children {
  display: grid;
  grid-template-columns: repeat(var(--jobtree-columns), 1fr);
  justify-items: center;
  width: 100%;
  gap: 0.8rem;
}

@keyframes jobtree-badge {
  from { opacity: 0; transform: scale(0.6); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes jobtree-cancel-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgb(217 119 6 / 55%); }
  50% { box-shadow: 0 0 0 0.7rem rgb(217 119 6 / 0%); }
}

@keyframes jobtree-finalise-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgb(14 127 179 / 50%); }
  50% { box-shadow: 0 0 0 0.7rem rgb(14 127 179 / 0%); }
}

@keyframes jobtree-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgb(121 84 246 / 35%); }
  50% { box-shadow: 0 0 0 0.45rem rgb(121 84 246 / 0%); }
}

html.dark .jobtree-node { background: rgb(31 32 35 / 92%); }
html.dark .jobtree-stack { background: rgb(170 138 255 / 10%); }
html.dark .jobtree-node.is-done { color: #7fe0bd; }
html.dark .jobtree-node.is-failed { color: #ff8fb0; }
html.dark .jobtree-node.is-cancelled { color: #b9bbc2; }
html.dark .jobtree-node.is-cancelling { color: #fcd34d; }
html.dark .jobtree-node.is-finalising { color: #8fd6ff; }

@media (prefers-reduced-motion: reduce) {
  .jobtree-node.is-running,
  .jobtree-node.is-finalising,
  .jobtree-node.is-cancelling { animation: none; }
  .jobtree-node, .jobtree-label, .jobtree-fan path { transition-delay: 0ms; }
  .jobtree-node::after { animation: none; }
}
</style>
