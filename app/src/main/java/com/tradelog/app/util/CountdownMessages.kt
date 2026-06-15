package com.tradelog.app.util

/** Short, serious daily trading pushes based on days remaining to a goal. */
object CountdownMessages {

    fun push(daysLeft: Int): String = when {
        daysLeft < 0 -> "Deadline passed. Review it honestly — what worked, what didn't, what improves next."
        daysLeft == 0 -> "Final day. Execute the plan. No revenge, no gambles."
        daysLeft == 1 -> "1 day left. Lock in. Only your A+ setup matters now."
        daysLeft <= 3 -> "$daysLeft days left. Tighten up — quality over quantity."
        daysLeft <= 7 -> "$daysLeft days left. Protect capital today. One clean setup beats emotional trades."
        daysLeft <= 14 -> "$daysLeft days left. Stay patient. Process over outcome."
        else -> "$daysLeft days left. Build the habit — show up and follow your rules."
    }

    fun daysLeftLabel(daysLeft: Int, title: String): String = when {
        daysLeft < 0 -> "Deadline passed — $title"
        daysLeft == 0 -> "Today is the day — $title"
        daysLeft == 1 -> "1 day left to $title"
        else -> "$daysLeft days left to $title"
    }

    fun backtestReminder(didYesterday: Boolean): String =
        if (didYesterday) "Good — you backtested yesterday. Keep the streak: do today's."
        else "You didn't upload any backtest yesterday. One backtest a day sharpens your edge — do one today."
}
