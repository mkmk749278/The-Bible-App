package com.manna.bible.domain.model

/**
 * The parsed result of a bundled canon definition asset.
 *
 * This is a lightweight, pure-Kotlin representation of a single canon's
 * metadata (its [canonType], [numberingScheme], and ordered [books]) produced by
 * the data layer's `CanonDefinitionDataSource`. It carries no Android dependencies
 * so it can be consumed by the pure-domain `CanonEngine` and exercised in JVM unit
 * tests without an emulator.
 *
 * @property canonType The canon this definition describes.
 * @property numberingScheme The Psalm/verse numbering convention for the canon.
 * @property books The ordered list of books that make up the canon.
 */
data class CanonDefinition(
    val canonType: CanonType,
    val numberingScheme: NumberingScheme,
    val books: List<CanonBook>
)
