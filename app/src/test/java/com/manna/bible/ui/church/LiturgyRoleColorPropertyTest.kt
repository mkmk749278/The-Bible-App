package com.manna.bible.ui.church

import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.ui.theme.DarkMannaPalette
import com.manna.bible.ui.theme.LightMannaPalette
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Property test for the role-color mapping ([liturgyRoleColor]) — pure JVM, no emulator.
 */
class LiturgyRoleColorPropertyTest {

    /** The four speaking roles that must each be visually distinct (rubric is not a speaker). */
    private val speakingRoles = listOf(
        LiturgyRole.PRESIDER,
        LiturgyRole.PEOPLE,
        LiturgyRole.ALL,
        LiturgyRole.READER
    )

    @Test
    fun `distinct speaking roles map to distinct, defined colors in every palette`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 11: For any two distinct roles from PRESIDER, PEOPLE, ALL, READER, the role-color mapping is defined for each and differs between them.
        val palettes = Arb.of(LightMannaPalette, DarkMannaPalette)
        val roleA = Arb.of(speakingRoles)
        val roleB = Arb.of(speakingRoles)
        checkAll(200, palettes, roleA, roleB) { palette, a, b ->
            val colorA = liturgyRoleColor(a, palette)
            val colorB = liturgyRoleColor(b, palette)
            if (a != b) {
                assertNotEquals(
                    colorA, colorB,
                    "roles $a and $b must have distinct colors in $palette"
                )
            }
        }
    }
}
