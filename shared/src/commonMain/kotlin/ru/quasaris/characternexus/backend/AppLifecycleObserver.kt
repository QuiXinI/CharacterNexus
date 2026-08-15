package ru.quasaris.characternexus.backend

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ru.quasaris.characternexus.util.Logger

class AppLifecycleObserver(
    private val repository: CharacterRepository
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        Logger.d("AppLifecycleObserver", "App moved to background, flushing data...")
        repository.flush()
    }
}
