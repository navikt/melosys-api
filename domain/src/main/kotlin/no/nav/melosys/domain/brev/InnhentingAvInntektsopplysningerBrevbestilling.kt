package no.nav.melosys.domain.brev

class InnhentingAvInntektsopplysningerBrevbestilling : DokgenBrevbestilling {
    // Beholdes utelukkende som diskriminator for polymorf deserialisering: DokgenBrevbestilling bruker
    // JsonTypeInfo.Id.DEDUCTION, og uten et felt som er unikt for subtypen blir den ikke entydig og
    // faller til defaultImpl (se DokgenBrevbestillingTest). Ingen brev-effekt etter MELOSYS-8158, settes ikke lenger.
    var skalViseStandardTekstOmOpplysninger: Boolean = false
    var fritekst: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fritekst = builder.fritekst
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var fritekst: String? = null

        constructor()

        constructor(brevbestilling: InnhentingAvInntektsopplysningerBrevbestilling) : super(brevbestilling) {
            this.fritekst = brevbestilling.fritekst
        }

        fun medFritekst(fritekst: String?) = apply { this.fritekst = fritekst }

        override fun build(): InnhentingAvInntektsopplysningerBrevbestilling = InnhentingAvInntektsopplysningerBrevbestilling(this)
    }
}
