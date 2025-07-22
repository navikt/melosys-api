package no.nav.melosys.domain.brev

import java.time.LocalDate

class VedtakOpphoertMedlemskapBrevbestilling : DokgenBrevbestilling {
    var opphørtBegrunnelseFritekst: String? = null
    var opphørtDato: LocalDate? = null
    var behandlingstema: String? = null
    var land: List<String>? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.opphørtBegrunnelseFritekst = builder.opphørtBegrunnelseFritekst
        this.opphørtDato = builder.opphørtDato
        this.behandlingstema = builder.behandlingstema
        this.land = builder.land
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var opphørtBegrunnelseFritekst: String? = null
        internal var opphørtDato: LocalDate? = null
        internal var behandlingstema: String? = null
        internal var land: List<String>? = null

        constructor()

        constructor(vedtakOpphoertMedlemskapBrevbestilling: VedtakOpphoertMedlemskapBrevbestilling) : super(vedtakOpphoertMedlemskapBrevbestilling) {
            this.opphørtBegrunnelseFritekst = vedtakOpphoertMedlemskapBrevbestilling.opphørtBegrunnelseFritekst
            this.opphørtDato = vedtakOpphoertMedlemskapBrevbestilling.opphørtDato
            this.behandlingstema = vedtakOpphoertMedlemskapBrevbestilling.behandlingstema
            this.land = vedtakOpphoertMedlemskapBrevbestilling.land
        }

        fun medOpphørtBegrunnelseFritekst(opphørtBegrunnelseFritekst: String?) = apply { this.opphørtBegrunnelseFritekst = opphørtBegrunnelseFritekst }

        fun medOpphørtDato(opphørtDato: LocalDate?) = apply { this.opphørtDato = opphørtDato }

        fun medBehandlingstema(behandlingstema: String?) = apply { this.behandlingstema = behandlingstema }

        fun medLand(land: List<String>?) = apply { this.land = land }

        override fun build(): VedtakOpphoertMedlemskapBrevbestilling = VedtakOpphoertMedlemskapBrevbestilling(this)
    }
}
