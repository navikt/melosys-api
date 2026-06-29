package no.nav.melosys.domain.brev

class InnhentingAvInntektsopplysningerBrevbestilling : DokgenBrevbestilling {
    // Beholdes utelukkende som diskriminator for polymorf deserialisering: DokgenBrevbestilling bruker
    // JsonTypeInfo.Id.DEDUCTION, og uten et felt som er unikt for denne subtypen blir den ikke entydig
    // (kun "fritekst" igjen, som deles med bl.a. FritekstbrevBrevbestilling) og faller til defaultImpl.
    // Se DokgenBrevbestillingTest. Flagget har ingen brev-effekt etter MELOSYS-8158 og settes ikke lenger.
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
