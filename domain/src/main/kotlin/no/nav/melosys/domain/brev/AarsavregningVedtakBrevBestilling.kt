package no.nav.melosys.domain.brev

class AarsavregningVedtakBrevBestilling : DokgenBrevbestilling {
    var innledningFritekst: String? = null
    var begrunnelseFritekst: String? = null


    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.innledningFritekst = builder.innledningFritekst
        this.begrunnelseFritekst = builder.begrunnelseFritekst
    }

    class Builder : DokgenBrevbestilling.Builder<Builder?> {
        internal var innledningFritekst: String? = null
        internal var begrunnelseFritekst: String? = null

        constructor()

        constructor(aarsavregningVedtakBrevBestilling: AarsavregningVedtakBrevBestilling) : super(
            aarsavregningVedtakBrevBestilling
        ) {
            this.innledningFritekst = aarsavregningVedtakBrevBestilling.innledningFritekst
            this.begrunnelseFritekst = aarsavregningVedtakBrevBestilling.begrunnelseFritekst
        }

        fun medInnledningFritekst(innledningFritekst: String?): Builder {
            this.innledningFritekst = innledningFritekst
            return this
        }

        fun medBegrunnelseFritekst(begrunnelseFritekst: String?): Builder {
            this.begrunnelseFritekst = begrunnelseFritekst
            return this
        }

        override fun build(): AarsavregningVedtakBrevBestilling {
            return AarsavregningVedtakBrevBestilling(this)
        }
    }
}
