/**
 * Auto-refresh cadence for the dashboard and orders screens: 10 minutes.
 *
 * Shared by the stores (stale-cache TTL) and the views (polling interval) so the
 * background refresh and the cache expiry always stay on the same clock.
 */
export const REFRESH_INTERVAL_MS = 10 * 60 * 1000
