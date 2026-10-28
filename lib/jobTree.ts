/**
 * The model behind `<JobTree>`: a tree of coroutines, scopes and resources
 * staged by the slide's clicks.
 *
 * Every node names the click at which it starts, completes, fails or is
 * cancelled. A node without `at` is running from the initial state; a node
 * without an end keeps running. The slide declares the click count itself
 * (`clicks: N` in its frontmatter), so annotations and the tree share one
 * timeline.
 */
export type JobKind =
  /** A coroutine: `launch`, `async`, or the scope that owns them. */
  | 'job'
  /** A suspending scope such as `resourceScope`: not a child Job, a call on the stack. */
  | 'scope'
  /** Something `install`ed in a `ResourceScope`: acquired, then released. */
  | 'resource'

export interface JobNode {
  label: string
  kind?: JobKind
  /** Click at which it starts (a resource: is acquired); absent means from click 0. */
  at?: number
  /** Click at which it completes (a resource: is released). */
  done?: number
  /** Click at which it throws. */
  failed?: number
  /** Click at which cancellation reaches it; it is still finishing. */
  cancelling?: number
  /** Click at which its clean-up runs: a resource's release, a `finally` block. */
  finalising?: number
  /** How the scope around a resource ended, named in its release caption; without it the caption just says `releasing`. */
  exit?: 'Completed' | 'Cancelled' | 'Failure'
  /** Position among the resources of its scope, 1-based: they release in reverse. */
  order?: number
  /** Click at which it has finished cancelling. Without `cancelling`, both happen at once. */
  cancelled?: number
  children?: JobNode[]
}

export type State = 'pending' | 'running' | 'done' | 'failed' | 'cancelling' | 'finalising' | 'cancelled'

export function stateOf(node: JobNode, click: number): State {
  if (node.failed !== undefined && click >= node.failed) return 'failed'
  if (node.cancelled !== undefined && click >= node.cancelled) return 'cancelled'
  if (node.done !== undefined && click >= node.done) return 'done'
  if (node.finalising !== undefined && click >= node.finalising) return 'finalising'
  if (node.cancelling !== undefined && click >= node.cancelling) return 'cancelling'
  if (click >= (node.at ?? 0)) return 'running'
  return 'pending'
}

export const ordinal = (n: number) =>
  `${n}${['th', 'st', 'nd', 'rd'][(n % 100 > 10 && n % 100 < 14) ? 0 : Math.min(n % 10, 4) % 4] ?? 'th'}`

export const broken = (state: State) =>
  state === 'failed' || state === 'cancelling' || state === 'cancelled'

/**
 * Siblings that change state on the same click do not flip in unison: each child
 * gets a fixed, seemingly random delay (golden-ratio spacing over `STAGGER_SPAN_MS`),
 * so the whole wave still settles within about 400 ms including the transition.
 */
const STAGGER_SPAN_MS = 200
export const stagger = (index: number) =>
  `${Math.round(((index * 0.6180339887) % 1) * STAGGER_SPAN_MS)}ms`

export const describe = (node: JobNode): string => {
  const children = node.children ?? []
  if (children.length === 0) return node.label
  return `${node.label} owns ${children.map(describe).join(', ')}`
}
