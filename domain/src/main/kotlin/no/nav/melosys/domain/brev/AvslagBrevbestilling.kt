package no.nav.melosys.domain.brev

class AvslagBrevbestilling : DokgenBrevbestilling {
    var avslagFritekst: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.avslagFritekst = builder.fritekst
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var fritekst: String? = null

        constructor()

        constructor(avslagBrevbestilling: AvslagBrevbestilling) : super(avslagBrevbestilling) {
            this.fritekst = avslagBrevbestilling.avslagFritekst
        }

        fun medFritekst(fritekst: String?) = apply { this.fritekst = fritekst }

        override fun build(): AvslagBrevbestilling = AvslagBrevbestilling(this)
    }
}
