<script setup lang="ts">
import { computed } from 'vue'
import { useSlideContext } from '@slidev/client'
import { describe, type JobNode } from '../lib/jobTree'

/**
 * The Job hierarchy beside a snippet, staged by the slide's clicks. See
 * `lib/jobTree.ts` for the node model; nodes nest through `children`, and a node's
 * `kind` tells a coroutine from a suspending scope from an installed resource.
 */
const props = withDefaults(defineProps<{
  root: JobNode
  /** Shorthand for `root.children`. */
  children?: JobNode[]
  /** Click at which `cancel()` arrives at the root from outside. */
  cancel?: number
  /** What arrives at the root: `cancel()` by default, `SIGTERM` for a process. */
  cancelLabel?: string
}>(), { children: () => [], cancelLabel: 'cancel()' })

const { $clicks } = useSlideContext()

const tree = computed<JobNode>(() => ({
  ...props.root,
  children: props.root.children ?? props.children,
}))
const cancelling = computed(() => props.cancel !== undefined && $clicks.value >= props.cancel)
const description = computed(() => describe(tree.value))
</script>

<template>
  <div class="jobtree" role="img" :aria-label="description">
    <div class="jobtree-cancel" :class="{ 'is-visible': cancelling }" aria-hidden="true">
      <span>{{ cancelLabel }}</span>
      <svg viewBox="0 0 12 20"><path d="M6 0 V15 M1 11 L6 17 L11 11" /></svg>
    </div>
    <JobTreeNode :node="tree" />
  </div>
</template>

<style scoped>
.jobtree {
  --jobtree-ink: var(--fundamentals-ink, #1f2023);
  --jobtree-running: var(--fundamentals-purple, #7954f6);
  --jobtree-done: #2fa37f;
  --jobtree-failed: #d3325a;
  --jobtree-cancelled: var(--fundamentals-cancel, #d97706);
  --jobtree-finalising: var(--fundamentals-finalise, #0e7fb3);
  position: relative;
  display: grid;
  justify-items: center;
  width: 100%;
  font-family: var(--slidev-font-mono);
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.jobtree-cancel {
  position: absolute;
  top: -3.2rem;
  font-size: 1.3rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  left: 50%;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transform: translateX(3rem);
  color: var(--jobtree-cancelled);
  opacity: 0;
  transition: opacity 300ms ease;
}
.jobtree-cancel.is-visible { opacity: 1; }
.jobtree-cancel svg {
  width: 1.2rem;
  height: 2.8rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
