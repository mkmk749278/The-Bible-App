package com.manna.bible.domain.liturgy

import com.manna.bible.domain.model.Denomination
import javax.inject.Inject

/**
 * Curated, offline orders of worship for Church Mode.
 *
 * Content policy (per CLAUDE.md — never invent liturgy): each order carries the
 * **structure**, the **rubrics** (who does what), the short **congregational
 * responses**, and the **public-domain / traditional ordinary texts** (the Lord's
 * Prayer, the Creed openings, the Sanctus, etc.). The presidential prayers proper to
 * the day (Collect, Preface, Eucharistic / Thanksgiving prayer, Prayer after
 * Communion) are intentionally left as flagged rubrics so the parish's official
 * book / Missal is followed. Sources are recorded on each [Liturgy.sourceNote].
 *
 *  - Roman Catholic Mass — structure per the USCCB Order of Mass; ordinary texts in
 *    the traditional / ecumenical (ICET) English.
 *  - Church of South India, Holy Communion — structure per the CSI Book of Common
 *    Worship (The Lord's Supper, 1950/1954); texts in traditional English.
 */
class DefaultLiturgyProvider @Inject constructor() : LiturgyProvider {

    override fun all(): List<Liturgy> = listOf(ROMAN_CATHOLIC_MASS, CSI_HOLY_COMMUNION)

    override fun defaultFor(denomination: Denomination): Liturgy? = when (denomination) {
        Denomination.CATHOLIC -> ROMAN_CATHOLIC_MASS
        Denomination.CSI, Denomination.PROTESTANT_OTHER -> CSI_HOLY_COMMUNION
        // Orthodox & Mar Thoma Qurbana are distinct rites we don't yet ship faithfully;
        // the screen offers the available orders rather than misrepresenting them.
        Denomination.ORTHODOX, Denomination.MAR_THOMA, Denomination.SHOW_EVERYTHING -> null
    }

    private companion object {

        // --- small builders to keep the orders readable ----------------------
        fun presider(title: String? = null, text: String) = LiturgyPart(LiturgyRole.PRESIDER, title, text)
        fun people(text: String) = LiturgyPart(LiturgyRole.PEOPLE, text = text)
        fun all(title: String? = null, text: String) = LiturgyPart(LiturgyRole.ALL, title, text)
        fun reader(text: String) = LiturgyPart(LiturgyRole.READER, text = text)
        fun rubric(text: String, title: String? = null) = LiturgyPart(LiturgyRole.RUBRIC, title = title, rubric = text)
        fun official(title: String, instruction: String) =
            LiturgyPart(LiturgyRole.RUBRIC, title = title, rubric = instruction, needsOfficialText = true)

        // ============================ ROMAN CATHOLIC MASS ============================
        val ROMAN_CATHOLIC_MASS = Liturgy(
            id = "roman_catholic_mass",
            title = "The Holy Mass",
            tradition = "Roman Catholic",
            sourceNote = "Order of Mass, Roman Rite — structure per the USCCB Order of Mass. " +
                "Ordinary texts in traditional / ecumenical (ICET) English. The prayers proper " +
                "to the day (Collect, Preface, Eucharistic Prayer, Prayer after Communion) follow " +
                "the parish Missal.",
            sections = listOf(
                LiturgySection(
                    "Introductory Rites",
                    listOf(
                        rubric("All stand. The Mass begins with an entrance hymn as the Priest and ministers approach the altar."),
                        presider("Sign of the Cross", "In the name of the Father, and of the Son, and of the Holy Spirit."),
                        people("Amen."),
                        presider("Greeting", "The grace of our Lord Jesus Christ, and the love of God, and the communion of the Holy Spirit be with you all."),
                        people("And with your spirit."),
                        rubric("All recall their sins and ask God's mercy.", title = "Penitential Act"),
                        all("Kyrie", "Lord, have mercy. Christ, have mercy. Lord, have mercy."),
                        all("Gloria", "Glory to God in the highest, and on earth peace to people of good will…"),
                        rubric("(The Gloria continues, and is sung on Sundays, solemnities and feasts.)"),
                        official("Collect", "The Priest sings or says the Collect — the opening prayer proper to the day."),
                        people("Amen.")
                    )
                ),
                LiturgySection(
                    "Liturgy of the Word",
                    listOf(
                        rubric("All sit. The appointed readings are proclaimed — see the day's readings in the Calendar.", title = "First Reading"),
                        reader("The word of the Lord."),
                        people("Thanks be to God."),
                        rubric("The Responsorial Psalm is sung or said, with the people responding."),
                        rubric("On Sundays and solemnities a Second Reading follows, ending: “The word of the Lord.” — “Thanks be to God.”", title = "Second Reading"),
                        all("Gospel Acclamation", "Alleluia, alleluia."),
                        rubric("All stand for the Gospel.", title = "Gospel"),
                        presider(text = "The Lord be with you."),
                        people("And with your spirit."),
                        presider(text = "A reading from the holy Gospel according to N."),
                        people("Glory to you, O Lord."),
                        reader("The Gospel of the Lord."),
                        people("Praise to you, Lord Jesus Christ."),
                        rubric("All sit. The Priest preaches, opening up the readings and applying them to life.", title = "Homily"),
                        all("The Nicene Creed", "I believe in one God, the Father almighty, maker of heaven and earth, of all things visible and invisible…"),
                        rubric("(All stand and profess the Creed in full on Sundays and solemnities.)"),
                        rubric("Intercessions are offered for the Church and the world.", title = "Universal Prayer"),
                        people("Lord, hear our prayer.")
                    )
                ),
                LiturgySection(
                    "Liturgy of the Eucharist",
                    listOf(
                        rubric("Bread and wine are brought to the altar; an offertory hymn may be sung.", title = "Presentation of the Gifts"),
                        presider(text = "Blessed are you, Lord God of all creation, for through your goodness we have received the bread we offer you…"),
                        people("Blessed be God for ever."),
                        official("Prayer over the Offerings", "The Priest prays over the gifts, proper to the day."),
                        people("Amen."),
                        presider("The Eucharistic Prayer", "The Lord be with you."),
                        people("And with your spirit."),
                        presider(text = "Lift up your hearts."),
                        people("We lift them up to the Lord."),
                        presider(text = "Let us give thanks to the Lord our God."),
                        people("It is right and just."),
                        official("Preface", "The Priest sings the Preface of thanksgiving, proper to the day."),
                        all("Holy, Holy, Holy (Sanctus)", "Holy, Holy, Holy Lord God of hosts. Heaven and earth are full of your glory. Hosanna in the highest. Blessed is he who comes in the name of the Lord. Hosanna in the highest."),
                        official("The Consecration", "The Priest continues the Eucharistic Prayer; recalling the Last Supper, the bread and wine become the Body and Blood of Christ."),
                        presider("The Mystery of Faith", "The mystery of faith."),
                        all(text = "We proclaim your Death, O Lord, and profess your Resurrection until you come again."),
                        rubric("The Eucharistic Prayer concludes with the Doxology: “…through him, with him, in him… for ever and ever.”"),
                        people("Amen."),
                        rubric("At the Saviour's command and formed by divine teaching, we dare to say:", title = "The Lord's Prayer"),
                        all(text = "Our Father, who art in heaven, hallowed be thy name; thy kingdom come, thy will be done, on earth as it is in heaven. Give us this day our daily bread, and forgive us our trespasses, as we forgive those who trespass against us; and lead us not into temptation, but deliver us from evil. Amen."),
                        presider("The Sign of Peace", "The peace of the Lord be with you always."),
                        people("And with your spirit."),
                        rubric("All offer one another a sign of peace."),
                        all("Lamb of God (Agnus Dei)", "Lamb of God, you take away the sins of the world, have mercy on us. Lamb of God, you take away the sins of the world, have mercy on us. Lamb of God, you take away the sins of the world, grant us peace."),
                        rubric("The faithful come forward to receive Holy Communion.", title = "Communion"),
                        presider(text = "The Body of Christ."),
                        people("Amen."),
                        official("Prayer after Communion", "The Priest prays the Prayer after Communion, proper to the day."),
                        people("Amen.")
                    )
                ),
                LiturgySection(
                    "Concluding Rites",
                    listOf(
                        presider("Greeting & Blessing", "The Lord be with you."),
                        people("And with your spirit."),
                        presider(text = "May almighty God bless you, the Father, and the Son, and the Holy Spirit."),
                        people("Amen."),
                        presider("Dismissal", "Go forth, the Mass is ended."),
                        people("Thanks be to God.")
                    )
                )
            )
        )

        // ========================= CSI HOLY COMMUNION ============================
        val CSI_HOLY_COMMUNION = Liturgy(
            id = "csi_holy_communion",
            title = "The Holy Communion",
            tradition = "Church of South India",
            sourceNote = "The Lord's Supper or the Holy Eucharist — structure per the Church of " +
                "South India Book of Common Worship (1950/1954). Texts in traditional English; " +
                "the Collect of the day and the Thanksgiving (Eucharistic) prayer follow the CSI " +
                "service book.",
            sections = listOf(
                LiturgySection(
                    "The Preparation",
                    listOf(
                        rubric("The people gather; a hymn of praise is sung."),
                        presider("Greeting", "The Lord be with you."),
                        people("And with thy spirit."),
                        all("Collect for Purity", "Almighty God, unto whom all hearts are open, all desires known, and from whom no secrets are hid: cleanse the thoughts of our hearts by the inspiration of thy Holy Spirit, that we may perfectly love thee, and worthily magnify thy holy name; through Christ our Lord. Amen."),
                        rubric("All kneel and make humble confession to Almighty God.", title = "Confession"),
                        all(text = "Almighty God, our heavenly Father, we have sinned against thee in thought, word, and deed. Have mercy upon us, forgive us our sins, and grant that we may serve thee in newness of life, to the glory of thy name. Amen."),
                        presider("Absolution", "Almighty God have mercy upon you, pardon and deliver you from all your sins, and bring you to everlasting life."),
                        people("Amen."),
                        all("Kyrie / Gloria", "Glory be to God on high, and on earth peace, good will toward men…")
                    )
                ),
                LiturgySection(
                    "The Ministry of the Word",
                    listOf(
                        official("Collect of the Day", "The minister says the Collect appointed for the day."),
                        people("Amen."),
                        rubric("The appointed Old Testament lesson and Epistle are read — see the day's readings in the Calendar.", title = "The Lessons"),
                        reader("This is the word of the Lord."),
                        people("Thanks be to God."),
                        rubric("All stand for the Gospel.", title = "The Gospel"),
                        people("Glory be to thee, O Lord."),
                        reader("This is the Gospel of Christ."),
                        people("Praise be to thee, O Christ."),
                        rubric("The minister preaches the Word, explaining the Scriptures.", title = "The Sermon"),
                        all("The Nicene Creed", "We believe in one God, the Father, the Almighty, maker of heaven and earth, of all that is, seen and unseen…"),
                        rubric("(All stand and confess the faith of the Church.)")
                    )
                ),
                LiturgySection(
                    "The Intercession",
                    listOf(
                        rubric("Prayers are offered for the Church, the nations, the community, and those in need.", title = "Prayers of the Church"),
                        presider(text = "Lord, in thy mercy:"),
                        people("Hear our prayer.")
                    )
                ),
                LiturgySection(
                    "The Lord's Supper",
                    listOf(
                        presider("The Peace", "The peace of the Lord be always with you."),
                        people("And with thy spirit."),
                        rubric("All greet one another with a sign of peace; the bread and wine are brought, and the offerings of the people gathered.", title = "The Offertory"),
                        presider("The Thanksgiving", "Lift up your hearts."),
                        people("We lift them up unto the Lord."),
                        presider(text = "Let us give thanks unto the Lord our God."),
                        people("It is meet and right so to do."),
                        all("Sanctus", "Holy, Holy, Holy, Lord God of hosts, heaven and earth are full of thy glory. Glory be to thee, O Lord most high. Amen."),
                        official("The Thanksgiving Prayer", "The presbyter continues the great Thanksgiving, giving thanks for creation and redemption and calling upon the Holy Spirit."),
                        LiturgyPart(
                            LiturgyRole.PRESIDER,
                            title = "The Words of Institution",
                            text = "Who, in the same night that he was betrayed, took bread; and when he had given thanks, he brake it, and gave it to his disciples, saying, Take, eat; this is my body which is given for you… This cup is the new covenant in my blood…",
                            osisRef = "1CO.11.23"
                        ),
                        all("The Lord's Prayer", "Our Father, who art in heaven, hallowed be thy name; thy kingdom come, thy will be done, on earth as it is in heaven. Give us this day our daily bread, and forgive us our trespasses, as we forgive those who trespass against us; and lead us not into temptation, but deliver us from evil. For thine is the kingdom, the power, and the glory, for ever and ever. Amen."),
                        presider("The Breaking of the Bread", "We being many are one body, for we all share in the one bread."),
                        rubric("The people come forward to receive the bread and the cup.", title = "The Communion"),
                        all("Thanksgiving after Communion", "Almighty and everliving God, we thank thee that thou dost feed us, who have duly received these holy mysteries, with the spiritual food of the body and blood of thy Son. Amen.")
                    )
                ),
                LiturgySection(
                    "The Conclusion",
                    listOf(
                        official("The Blessing", "The presbyter blesses the people in the name of the Father, and the Son, and the Holy Spirit."),
                        people("Amen."),
                        presider("Dismissal", "Go in peace, and serve the Lord."),
                        people("In the name of Christ. Amen.")
                    )
                )
            )
        )
    }
}
