package no.nav.melosys.domain.brev

class InnhentingAvInntektsopplysningerBrevbestilling : DokgenBrevbestilling {
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
