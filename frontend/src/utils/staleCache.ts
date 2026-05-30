export function createStaleCache(ttlMs: number) {
  let lastFetched: number | null = null
  return {
    isStale: () => lastFetched === null || Date.now() - lastFetched > ttlMs,
    markFresh: () => { lastFetched = Date.now() },
    invalidate: () => { lastFetched = null },
  }
}
