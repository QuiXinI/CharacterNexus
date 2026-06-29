package ru.quasaris.characters.master

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(
    private val repository: CharacterRepository
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "App moved to background, flushing data...")
        repository.flush()
    }
}
