package com.applimit.data.db

/**
 * The four app categories the parent assigns.
 *
 *  - BLOCKED  : always locked, overlay appears immediately, counts to no limit.
 *  - PLUS      : "Zugelassen Plus" – never blocked. Does NOT count to the general
 *               limit. Counts to the GLOBAL limit only if [ManagedApp.plusCountsToGlobal].
 *  - LIMIT     : has its own individual daily limit ([ManagedApp.individualLimitMinutes]).
 *               Counts to BOTH the general and the global limit. Blocked when the
 *               individual OR general OR global limit is reached.
 *  - STANDARD  : counts to BOTH general and global limit. Blocked when the general
 *               OR global limit is reached.
 *
 * Apps not present in the table are treated as unmanaged → always allowed and
 * counted to nothing (so a freshly installed app never surprise-locks the phone).
 */
enum class AppCategory {
    BLOCKED,
    PLUS,
    LIMIT,
    STANDARD,
}
