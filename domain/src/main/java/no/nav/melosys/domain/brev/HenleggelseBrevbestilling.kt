package no.nav.melosys.domain.brev

class HenleggelseBrevbestilling : DokgenBrevbestilling {
    var fritekst: String? = null
    var begrunnelseKode: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fritekst = builder.fritekst
        this.begrunnelseKode = builder.begrunnelseKode
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var fritekst: String? = null
        internal var begrunnelseKode: String? = null

        constructor()

        constructor(henleggelseBrevbestilling: HenleggelseBrevbestilling) : super(henleggelseBrevbestilling) {
            this.fritekst = henleggelseBrevbestilling.fritekst
            this.begrunnelseKode = henleggelseBrevbestilling.begrunnelseKode
        }

        fun medFritekst(fritekst: String?) = apply { this.fritekst = fritekst }

        fun medBegrunnelseKode(begrunnelseKode: String?) = apply { this.begrunnelseKode = begrunnelseKode }

        override fun build(): HenleggelseBrevbestilling = HenleggelseBrevbestilling(this)
    }
}
