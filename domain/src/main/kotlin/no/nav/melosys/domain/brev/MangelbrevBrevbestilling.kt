package no.nav.melosys.domain.brev

import com.fasterxml.jackson.annotation.JsonAlias

class MangelbrevBrevbestilling : DokgenBrevbestilling {
    var manglerInfoFritekst: String? = null
    var innledningFritekst: String? = null
    var fullmektigNavn: String? = null

    @JsonAlias("isBrukerSkalHaKopi") // for reading old java format
    var brukerSkalHaKopi: Boolean = false

    // Add this method for Java compatibility
    fun isBrukerSkalHaKopi() = brukerSkalHaKopi

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.manglerInfoFritekst = builder.manglerInfoFritekst
        this.innledningFritekst = builder.innledningFritekst
        this.fullmektigNavn = builder.fullmektigNavn
        this.brukerSkalHaKopi = builder.brukerSkalHaKopi
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var manglerInfoFritekst: String? = null
        internal var innledningFritekst: String? = null
        internal var fullmektigNavn: String? = null
        internal var brukerSkalHaKopi: Boolean = false

        constructor()

        constructor(mangelbrevBrevbestilling: MangelbrevBrevbestilling) : super(mangelbrevBrevbestilling) {
            this.manglerInfoFritekst = mangelbrevBrevbestilling.manglerInfoFritekst
            this.innledningFritekst = mangelbrevBrevbestilling.innledningFritekst
            this.fullmektigNavn = mangelbrevBrevbestilling.fullmektigNavn
            this.brukerSkalHaKopi = mangelbrevBrevbestilling.brukerSkalHaKopi
        }

        fun medManglerInfoFritekst(manglerInfoFritekst: String?) = apply {
            this.manglerInfoFritekst = manglerInfoFritekst
        }

        fun medInnledningFritekst(innledningFritekst: String?) = apply {
            this.innledningFritekst = innledningFritekst
        }

        fun medFullmektigNavn(fullmektigNavn: String?) = apply {
            this.fullmektigNavn = fullmektigNavn
        }

        fun medBrukerSkalHaKopi(brukerSkalHaKopi: Boolean) = apply {
            this.brukerSkalHaKopi = brukerSkalHaKopi
        }

        override fun build(): MangelbrevBrevbestilling {
            return MangelbrevBrevbestilling(this)
        }
    }
}
