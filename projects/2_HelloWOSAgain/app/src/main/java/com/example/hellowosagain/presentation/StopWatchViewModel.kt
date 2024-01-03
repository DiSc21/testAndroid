package com.example.hellowosagain.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellowosagain.presentation.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StopWatchViewModel: ViewModel() {

    private val elapsedTime_ = MutableStateFlow(0L)
    private val timerState_ = MutableStateFlow(TimerState.RESET)

    val timer_state_ = timerState_.asStateFlow()

    private val formatter_ = DateTimeFormatter.ofPattern("HH' h\n'mm' min\n'ss.SSS' sec'")

    val stopWatchText = elapsedTime_
        .map {
            millis -> LocalTime.ofNanoOfDay(millis * 1_000_000).format(formatter_)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "00:00:00:000"
        )

    init {
        timerState_
            .flatMapLatest { timer_state_ -> getTimerFlow(isRunning = timer_state_ == TimerState.RUNNING) }
            .onEach {
                timeDiff -> elapsedTime_.update { it + timeDiff }
            }
            .launchIn(viewModelScope)
    }

    fun toggleIsRunning() {
        when(timerState_.value) {
            TimerState.RUNNING -> timerState_.update {TimerState.PAUSED}
            TimerState.PAUSED,
            TimerState.RESET -> timerState_.update { TimerState.RUNNING }
        }
    }

    fun resetTimer() {
        timerState_.update {TimerState.RESET }
        elapsedTime_.update { 0L }
    }
    private fun getTimerFlow(isRunning: Boolean): Flow<Long> {
        return flow {
            var startMillis = System.currentTimeMillis()
            while(isRunning) {
                val currentMillis = System.currentTimeMillis()
                val timeDiff = if(currentMillis > startMillis) {
                    currentMillis - startMillis
                } else {
                    0L
                }
                emit(timeDiff)
                startMillis = System.currentTimeMillis()
                delay(10L)
            }
        }
    }
}