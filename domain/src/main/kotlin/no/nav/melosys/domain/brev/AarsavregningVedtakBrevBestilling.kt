package no.nav.melosys.domain.brev

class AarsavregningVedtakBrevBestilling : DokgenBrevbestilling {
    var innledningFritekstAarsavregning: String? = null
    var begrunnelseFritekstAarsavregning: String? = null


    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.innledningFritekstAarsavregning = builder.innledningFritekst
        this.begrunnelseFritekstAarsavregning = builder.begrunnelseFritekst
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder?> {
        internal var innledningFritekst: String? = null
        internal var begrunnelseFritekst: String? = null

        constructor()

        constructor(aarsavregningVedtakBrevBestilling: AarsavregningVedtakBrevBestilling) : super(
            aarsavregningVedtakBrevBestilling
        ) {
            this.innledningFritekst = aarsavregningVedtakBrevBestilling.innledningFritekstAarsavregning
            this.begrunnelseFritekst = aarsavregningVedtakBrevBestilling.begrunnelseFritekstAarsavregning
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
