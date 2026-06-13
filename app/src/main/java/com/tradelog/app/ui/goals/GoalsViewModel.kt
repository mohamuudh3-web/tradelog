package com.tradelog.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.GoalMetric
import com.tradelog.app.data.entity.GoalType
import com.tradelog.app.data.entity.TaskFrequency
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.ui.dashboard.GoalProgressUi
import com.tradelog.app.ui.dashboard.TaskUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val goals: StateFlow<List<GoalProgressUi>> = combine(
        repo.allGoals, repo.trades, repo.journals, repo.tasks
    ) { goals, _, _, _ ->
        goals.map { GoalProgressUi(it, repo.goalProgress(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskUi>> =
        repo.tasks.map { list -> list.map { TaskUi(it, repo.isTaskDoneToday(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(title: String, type: GoalType, metric: GoalMetric, target: Int, unit: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.saveGoal(Goal(title = title.trim(), type = type, metric = metric, target = target.coerceAtLeast(1), unit = unit.trim()))
        }
    }

    fun increment(goal: Goal, delta: Int) = viewModelScope.launch { repo.incrementGoal(goal, delta) }
    fun archive(goal: Goal, archived: Boolean) = viewModelScope.launch { repo.setGoalArchived(goal, archived) }
    fun deleteGoal(goal: Goal) = viewModelScope.launch { repo.deleteGoal(goal) }

    fun addTask(title: String, frequency: TaskFrequency) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.saveTask(TaskItem(title = title.trim(), frequency = frequency)) }
    }

    fun toggleTask(task: TaskItem, done: Boolean) = viewModelScope.launch { repo.setTaskDone(task, done) }
    fun deleteTask(task: TaskItem) = viewModelScope.launch { repo.deleteTask(task) }
}
