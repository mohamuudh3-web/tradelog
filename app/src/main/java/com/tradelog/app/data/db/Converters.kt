package com.tradelog.app.data.db

import androidx.room.TypeConverter
import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.GoalMetric
import com.tradelog.app.data.entity.GoalType
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.data.entity.PayoutStatus
import com.tradelog.app.data.entity.TaskCategory
import com.tradelog.app.data.entity.TaskFrequency
import com.tradelog.app.data.entity.TradeResult

/** Stores enums as their name string. */
class Converters {
    @TypeConverter fun directionTo(v: Direction): String = v.name
    @TypeConverter fun directionFrom(v: String): Direction = Direction.valueOf(v)

    @TypeConverter fun resultTo(v: TradeResult): String = v.name
    @TypeConverter fun resultFrom(v: String): TradeResult = TradeResult.valueOf(v)

    @TypeConverter fun goalTypeTo(v: GoalType): String = v.name
    @TypeConverter fun goalTypeFrom(v: String): GoalType = GoalType.valueOf(v)

    @TypeConverter fun goalMetricTo(v: GoalMetric): String = v.name
    @TypeConverter fun goalMetricFrom(v: String): GoalMetric = GoalMetric.valueOf(v)

    @TypeConverter fun taskFreqTo(v: TaskFrequency): String = v.name
    @TypeConverter fun taskFreqFrom(v: String): TaskFrequency = TaskFrequency.valueOf(v)

    @TypeConverter fun taskCategoryTo(v: TaskCategory): String = v.name
    @TypeConverter fun taskCategoryFrom(v: String): TaskCategory = TaskCategory.valueOf(v)

    @TypeConverter fun payoutStatusTo(v: PayoutStatus): String = v.name
    @TypeConverter fun payoutStatusFrom(v: String): PayoutStatus = PayoutStatus.valueOf(v)

    @TypeConverter fun impactTo(v: Impact): String = v.name
    @TypeConverter fun impactFrom(v: String): Impact = Impact.valueOf(v)
}
