package no.nav.melosys.domain.brev

class InnhentingAvInntektsopplysningerBrevbestilling : DokgenBrevbestilling {
    var skalViseStandardTekstOmOpplysninger: Boolean = false
    var fritekst: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.skalViseStandardTekstOmOpplysninger = builder.skalViseStandardTekstOmOpplysninger
        this.fritekst = builder.fritekst
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var skalViseStandardTekstOmOpplysninger: Boolean = false
        internal var fritekst: String? = null

        constructor()

        constructor(brevbestilling: InnhentingAvInntektsopplysningerBrevbestilling) : super(brevbestilling) {
            this.skalViseStandardTekstOmOpplysninger = brevbestilling.skalViseStandardTekstOmOpplysninger
            this.fritekst = brevbestilling.fritekst
        }

        fun medSkalViseStandardTekstOmOpplysninger(skalViseStandardTekstOmOpplysninger: Boolean) = apply { this.skalViseStandardTekstOmOpplysninger = skalViseStandardTekstOmOpplysninger }

        fun medFritekst(fritekst: String?) = apply { this.fritekst = fritekst }

        override fun build(): InnhentingAvInntektsopplysningerBrevbestilling = InnhentingAvInntektsopplysningerBrevbestilling(this)
    }
}
