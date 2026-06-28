package com.charles.nutrisnap.data

import java.util.TimeZone

/** Milliseconds in a calendar day. */
const val MS_PER_DAY: Long = 86_400_000L

/**
 * The device's local timezone offset (including DST) at [atMs], in milliseconds.
 *
 * Meals are bucketed by the user's *local* calendar day, not UTC. Using a raw
 * `timestampMs / MS_PER_DAY` (UTC) makes the day-key roll over mid-evening for
 * anyone west of UTC, so a meal logged at, say, 8pm local would land on the next
 * day's bucket and "disappear" from today's diary.
 */
fun tzOffsetMs(atMs: Long = System.currentTimeMillis()): Long =
    TimeZone.getDefault().getOffset(atMs).toLong()

/** Today as a local epoch-day (days since 1970 in the device's timezone). */
fun localEpochDay(atMs: Long = System.currentTimeMillis()): Long =
    Math.floorDiv(atMs + tzOffsetMs(atMs), MS_PER_DAY)

/**
 * The `[start, end)` instant range (ms) covering local calendar day [epochDay].
 * [offsetMs] defaults to the current offset so it stays consistent with the
 * grouping used by the DAO queries.
 */
fun localDayRangeMs(epochDay: Long, offsetMs: Long = tzOffsetMs()): Pair<Long, Long> {
    val start = epochDay * MS_PER_DAY - offsetMs
    return start to start + MS_PER_DAY
}
