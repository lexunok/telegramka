package ru.jarvis.telegramka.ui.verify

import androidx.lifecycle.ViewModel

class VerifyCodeViewModel : ViewModel() {
    fun isCodeValid(code: String): Boolean {
        return code.length == 6 && code.all(Char::isDigit)
    }
}
