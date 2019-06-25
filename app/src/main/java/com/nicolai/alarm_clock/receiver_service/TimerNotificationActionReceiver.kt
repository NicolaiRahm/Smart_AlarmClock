package com.nicolai.alarm_clock.receiver_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            AppConstantsKotlinTimer.ACTION_STOP -> {
                Timer.removeAlarm(context)
                PrefUtil.setTimerState(Timer.TimerState.Stopped, context)
                NotificationUtil.hideTimerNotification(context)
            }

            AppConstantsKotlinTimer.ACTION_PAUSE ->{
                var secondsRemaining = PrefUtil.getSecondsRemaing(context)
                val alarmSetTime = PrefUtil.getAlarmSetTime(context)
                val nowSeconds = Timer.nowSeconds

                //nowSeconds - alarmSetTime gleich seconds die in background vergangen sind
                secondsRemaining -= nowSeconds - alarmSetTime
                PrefUtil.setSecondsRemaing(secondsRemaining, context)

                Timer.removeAlarm(context)
                PrefUtil.setTimerState(Timer.TimerState.Paused, context)
                NotificationUtil.showTimerPaused(context)
            }

            AppConstantsKotlinTimer.ACTION_RESUME -> {
                val secondsRemaining = PrefUtil.getSecondsRemaing(context)
                val wakeUpTime = Timer.setAlarm(context, Timer.nowSeconds, secondsRemaining)

                PrefUtil.setTimerState(Timer.TimerState.Running, context)
                NotificationUtil.showTimerRunning(context, wakeUpTime)
            }

            AppConstantsKotlinTimer.ACTION_START -> {
                val minutesRemaining = PrefUtil.getTimerLength(context)
                val secondsRemaining = minutesRemaining * 60L
                val wakeUpTime = Timer.setAlarm(context, Timer.nowSeconds, secondsRemaining)

                PrefUtil.setTimerState(Timer.TimerState.Running, context)
                PrefUtil.setSecondsRemaing(secondsRemaining, context)
                NotificationUtil.showTimerRunning(context, wakeUpTime)
            }
        }
    }
}
