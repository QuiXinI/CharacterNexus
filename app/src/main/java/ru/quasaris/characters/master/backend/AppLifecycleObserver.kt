package ru.quasaris.characters.master.backend

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ru.quasaris.characters.master.CharacterRepository

class AppLifecycleObserver(
    private val repository: CharacterRepository
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "App moved to background, flushing data...")
        repository.flush()
    }
}
