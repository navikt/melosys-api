package no.nav.melosys.domain.brev

class HenleggelseBrevbestilling : DokgenBrevbestilling {
    var fritekst: String? = null
    var begrunnelseKode: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fritekst = builder.fritekst
        this.begrunnelseKode = builder.begrunnelseKode
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder?> {
        internal var fritekst: String? = null
        var begrunnelseKode: String? = null

        constructor()

        constructor(fritekstbrevBrevbestilling: HenleggelseBrevbestilling) : super(fritekstbrevBrevbestilling) {
            this.fritekst = fritekstbrevBrevbestilling.fritekst
            this.begrunnelseKode = fritekstbrevBrevbestilling.begrunnelseKode
        }

        fun medFritekst(fritekst: String?): Builder {
            this.fritekst = fritekst
            return this
        }

        fun medBegrunnelseKode(begrunnelseKode: String?): Builder {
            this.begrunnelseKode = begrunnelseKode
            return this
        }

        override fun build(): HenleggelseBrevbestilling {
            return HenleggelseBrevbestilling(this)
        }
    }
}
