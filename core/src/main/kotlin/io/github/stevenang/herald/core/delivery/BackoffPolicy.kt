package io.github.stevenang.herald.core.delivery

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Computes retry delays for failed deliveries using exponential backoff with
 * equal jitter.
 *
 * The delay for a given [attempt] (1-based) grows as `baseDelayMillis * 2^(attempt - 1)`,
 * capped at [maxDelayMillis]. Equal jitter then splits the capped delay in half:
 * the first half is kept fixed, and a random amount up to the second half is
 * added on top. This guarantees the returned delay always falls in
 * `[cappedDelay / 2, cappedDelay)` — enough randomness to spread out retries
 * that failed at the same time (avoiding a "thundering herd" against a
 * recovering endpoint), without ever retrying sooner than half the intended
 * backoff.
 *
 * This class only computes delays; it does not track attempt counts or decide
 * when to stop retrying. Callers are expected to check [isExhausted] before
 * calling [nextDelay] and to own the decision of what happens once a delivery
 * is exhausted (e.g. moving it to the dead-letter queue).
 *
 * See ADR-003 (at-least-once delivery semantics) for the reasoning behind
 * these retry guarantees.
 *
 * @property baseDelayMillis delay before the first retry, in milliseconds. Must be > 0.
 * @property maxDelayMillis upper bound on any computed delay, in milliseconds.
 *   Must be >= [baseDelayMillis].
 * @property maxAttempts maximum number of attempts (including the first) before
 *   a delivery is considered exhausted. Must be > 0.
 */
class BackoffPolicy(
    val baseDelayMillis: Long,
    val maxDelayMillis: Long,
    val maxAttempts: Int
) {
    init {
        require(baseDelayMillis > 0) { "baseDelayMillis must be greater than 0" }
        require(maxDelayMillis >= baseDelayMillis) { "maxDelayMillis must be greater than or equal to baseDelayMillis" }
        require(maxAttempts > 0) { "maxAttempts must be greater than 0" }
    }

    /**
     * Returns the delay to wait before retrying after [attempt] has failed.
     *
     * [attempt] is 1-based: `nextDelay(1, ...)` returns the delay before the
     * *second* try, computed from `baseDelayMillis * 2^0`.
     *
     * [random] is injectable so callers (in particular, tests) can get a
     * deterministic result from a seeded [Random] instead of [Random.Default].
     *
     * @throws IllegalArgumentException if [attempt] is not a positive number.
     */
    fun nextDelay(attempt: Int, random: Random = Random.Default): Duration {
        require(attempt > 0) { "attempt must be greater than 0" }
        // Raw exponential delay, uncapped — grows fast, so it must be capped
        // before any jitter math, or the final clamp would swallow all the
        // randomness added below (see the retry-storm scenario in the class doc).
        val delay = (baseDelayMillis * Math.pow(2.0, (attempt - 1).toDouble())).toLong()
        val cappedDelay = Math.min(delay, maxDelayMillis)

        // Equal jitter: half of the capped delay is fixed, half is random.
        // The guard on halfCappedDelay avoids calling Random.nextLong(0, 0),
        // which throws — possible for very small baseDelayMillis values.
        val halfCappedDelay = cappedDelay / 2
        val jitter = if (halfCappedDelay > 0) random.nextLong(0, halfCappedDelay) else 0L
        
        return (halfCappedDelay + jitter).milliseconds
    }

    fun isExhausted(attempt: Int): Boolean {
        return attempt >= maxAttempts
    }
}