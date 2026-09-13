package app.snake.core

import android.os.VibrationEffect
import android.os.Vibrator

enum class Buzz { Soft, Hit, Dead, Win }

fun Vibrator?.play(buzz: Buzz) {
    val effect = when (buzz) {
        Buzz.Soft -> VibrationEffect.createOneShot(22, 60)
        Buzz.Hit -> VibrationEffect.createOneShot(28, 90)
        Buzz.Dead -> VibrationEffect.createOneShot(70, 160)
        Buzz.Win -> VibrationEffect.createOneShot(120, 200)
    }
    this?.vibrate(effect)
}
