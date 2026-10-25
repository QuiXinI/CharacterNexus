package ru.quasaris.characternexus.backend.glossary

/**
 * Сборка списков заклинаний класса и подкласса.
 *
 * Класс получает заклинания из двух источников:
 *  1. явный список ClassDefinition.spellcasting.spellIds;
 *  2. заклинания, у которых glossaryClassIds содержит id класса.
 *
 * Подкласс получает только свои spellIds и заклинания из spellGrants.
 * Списки подклассов одного класса не пересекаются через этот резолвер,
 * потому что каждый подкласс читает только собственные поля.
 */
object SpellListResolver {

    /**
     * @param spellClassIndex карта «id заклинания -> glossaryClassIds этого заклинания».
     *        Строится один раз из всех SpellCard модуля и переиспользуется.
     */
    fun classSpellIds(
        cls: ClassDefinition,
        spellClassIndex: Map<String, Set<String>>
    ): Set<String> {
        val tagged = spellClassIndex
            .filterValues { cls.id in it }
            .keys
        return cls.spellcasting.spellIds.toSet() + tagged
    }

    /**
     * Все заклинания, которые доступны подклассу на любом уровне (явные + grants).
     */
    fun subclassSpellIds(sub: SubclassDefinition): Set<String> =
        sub.spellIds.toSet() + sub.spellGrants.flatMap { it.spellIds }

    /**
     * Заклинания, которые подкласс держит подготовленными при уровне класса [classLevel].
     */
    fun alwaysPreparedFor(sub: SubclassDefinition, classLevel: Int): Set<String> =
        sub.spellGrants
            .filter { it.classLevel <= classLevel }
            .flatMap { it.spellIds }
            .toSet()
}
