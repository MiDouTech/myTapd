interface InertiaWheelScrollController {
  destroy: () => void
}

interface InertiaWheelScrollOptions {
  lerpFactor?: number
  maxDeltaPerFrame?: number
}

const DEFAULT_LERP_FACTOR = 0.16
const DEFAULT_MAX_DELTA_PER_FRAME = 240
const LINE_HEIGHT_PX = 16

function isVerticalScrollable(element: HTMLElement): boolean {
  const style = window.getComputedStyle(element)
  const overflowY = style.overflowY
  const canScrollY = overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay'
  return canScrollY && element.scrollHeight > element.clientHeight + 1
}

function canScrollInDirection(element: HTMLElement, deltaY: number): boolean {
  if (deltaY > 0) {
    return element.scrollTop + element.clientHeight < element.scrollHeight - 1
  }
  if (deltaY < 0) {
    return element.scrollTop > 1
  }
  return false
}

function normalizeWheelDelta(deltaY: number, deltaMode: number, viewportHeight: number): number {
  if (deltaMode === 1) {
    return deltaY * LINE_HEIGHT_PX
  }
  if (deltaMode === 2) {
    return deltaY * viewportHeight
  }
  return deltaY
}

function findNestedScrollable(target: EventTarget | null, boundary: HTMLElement): HTMLElement | null {
  if (!(target instanceof HTMLElement)) {
    return null
  }
  let node: HTMLElement | null = target
  while (node && node !== boundary) {
    if (isVerticalScrollable(node)) {
      return node
    }
    node = node.parentElement
  }
  return null
}

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function createInertiaWheelScroll(
  container: HTMLElement,
  options: InertiaWheelScrollOptions = {},
): InertiaWheelScrollController {
  if (!container || prefersReducedMotion()) {
    return {
      destroy() {
        // No listeners were mounted.
      },
    }
  }

  const lerpFactor = options.lerpFactor ?? DEFAULT_LERP_FACTOR
  const maxDeltaPerFrame = options.maxDeltaPerFrame ?? DEFAULT_MAX_DELTA_PER_FRAME
  let targetScrollTop = container.scrollTop
  let rafId: number | null = null

  const stopAnimation = (): void => {
    if (rafId !== null) {
      window.cancelAnimationFrame(rafId)
      rafId = null
    }
  }

  const tick = (): void => {
    const current = container.scrollTop
    const distance = targetScrollTop - current

    if (Math.abs(distance) <= 0.5) {
      container.scrollTop = targetScrollTop
      rafId = null
      return
    }

    const frameStep = Math.max(-maxDeltaPerFrame, Math.min(maxDeltaPerFrame, distance * lerpFactor))
    container.scrollTop = current + frameStep
    rafId = window.requestAnimationFrame(tick)
  }

  const startAnimation = (): void => {
    if (rafId !== null) {
      return
    }
    rafId = window.requestAnimationFrame(tick)
  }

  const onWheel = (event: WheelEvent): void => {
    if (event.defaultPrevented || event.ctrlKey) {
      return
    }

    if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) {
      return
    }

    const nestedScrollable = findNestedScrollable(event.target, container)
    if (nestedScrollable && canScrollInDirection(nestedScrollable, event.deltaY)) {
      return
    }

    const maxScrollTop = Math.max(0, container.scrollHeight - container.clientHeight)
    if (maxScrollTop === 0) {
      return
    }

    const normalizedDelta = normalizeWheelDelta(event.deltaY, event.deltaMode, container.clientHeight)
    if (!canScrollInDirection(container, normalizedDelta)) {
      return
    }

    event.preventDefault()
    targetScrollTop = Math.max(0, Math.min(maxScrollTop, targetScrollTop + normalizedDelta))
    startAnimation()
  }

  const onNativeScroll = (): void => {
    // Keep target synced with direct scroll operations (drag scrollbar / keyboard / Home-End).
    if (rafId === null) {
      targetScrollTop = container.scrollTop
    }
  }

  const onTouchStart = (): void => {
    // Let touch interaction take over immediately to keep finger tracking natural.
    stopAnimation()
    targetScrollTop = container.scrollTop
  }

  container.addEventListener('wheel', onWheel, { passive: false })
  container.addEventListener('scroll', onNativeScroll, { passive: true })
  container.addEventListener('touchstart', onTouchStart, { passive: true })

  return {
    destroy() {
      stopAnimation()
      container.removeEventListener('wheel', onWheel)
      container.removeEventListener('scroll', onNativeScroll)
      container.removeEventListener('touchstart', onTouchStart)
    },
  }
}
