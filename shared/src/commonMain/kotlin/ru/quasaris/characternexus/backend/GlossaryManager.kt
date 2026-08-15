package ru.quasaris.characternexus.backend

class GlossaryManager(private val spellbookManager: SpellbookManager) {
    fun resolveRef(ref: String): Any? {
        return when {
            ref.startsWith("ref://spells/") -> spellbookManager.resolveRef(ref)
            // Future expansions:
            // ref.startsWith("ref://species/") -> speciesManager.resolve(ref)
            else -> null
        }
    }
}
