package com.tradelog.app.data.entity

enum class Direction { LONG, SHORT }

enum class TradeResult { WIN, LOSS, BREAKEVEN }

enum class GoalType { DAILY, WEEKLY }

/** How a goal's progress is measured. AUTO metrics are counted from logged records. */
enum class GoalMetric { MANUAL, TRADES, JOURNAL_ENTRIES, TASKS_COMPLETED }

enum class TaskFrequency { ONCE, DAILY }

enum class TaskCategory { TASK, ROUTINE }

enum class PayoutStatus { PENDING, PAID }

enum class Impact { LOW, MEDIUM, HIGH, HOLIDAY }
