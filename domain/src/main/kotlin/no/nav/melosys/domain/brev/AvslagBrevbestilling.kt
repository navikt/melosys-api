package no.nav.melosys.domain.brev

class AvslagBrevbestilling : DokgenBrevbestilling {
    var avslagFritekst: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.avslagFritekst = builder.fritekst
    }

    constructor(builder: Builder?, fritekst: String?) : super(builder) {
        this.avslagFritekst = fritekst
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder?> {
        internal var fritekst: String? = null

        constructor()

        constructor(fritekstbrevBrevbestilling: AvslagBrevbestilling) : super(fritekstbrevBrevbestilling) {
            this.fritekst = fritekstbrevBrevbestilling.avslagFritekst
        }

        fun medFritekst(fritekst: String?): Builder {
            this.fritekst = fritekst
            return this
        }

        override fun build(): AvslagBrevbestilling {
            return AvslagBrevbestilling(this)
        }
    }
}
