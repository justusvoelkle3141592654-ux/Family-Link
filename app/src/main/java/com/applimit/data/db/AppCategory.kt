package com.applimit.data.db

/**
 * The three app categories the parent assigns (Prompt Punkt 2).
 *
 *  - PLUS    : freely usable, does NOT count against the daily limit.
 *  - LIMITED : counts against the shared daily limit.
 *  - BLOCKED : generally not usable (always blocked by overlay).
 *
 * DECISION: the BLOCKED category is included by default (the brief described
 * three categories in Punkt 2). It can simply stay empty if unwanted.
 */
enum class AppCategory {
    PLUS,
    LIMITED,
    BLOCKED,
}
