package no.nav.melosys.domain.brev

import com.fasterxml.jackson.annotation.JsonAlias

class InnvilgelseBrevbestilling : DokgenBrevbestilling {
    var innledningFritekst: String? = null
    var begrunnelseFritekst: String? = null
    var ektefelleFritekst: String? = null
    var barnFritekst: String? = null

    @JsonAlias("isVirksomhetArbeidsgiverSkalHaKopi") // for reading old java format
    var virksomhetArbeidsgiverSkalHaKopi: Boolean = false
    var nyVurderingBakgrunn: String? = null

    // Add this method for Java compatibility
    fun isVirksomhetArbeidsgiverSkalHaKopi(): Boolean = virksomhetArbeidsgiverSkalHaKopi

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.innledningFritekst = builder.innledningFritekst
        this.begrunnelseFritekst = builder.begrunnelseFritekst
        this.ektefelleFritekst = builder.ektefelleFritekst
        this.barnFritekst = builder.barnFritekst
        this.virksomhetArbeidsgiverSkalHaKopi = builder.virksomhetArbeidsgiverSkalHaKopi
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var innledningFritekst: String? = null
        internal var begrunnelseFritekst: String? = null
        internal var ektefelleFritekst: String? = null
        internal var barnFritekst: String? = null
        internal var virksomhetArbeidsgiverSkalHaKopi: Boolean = false
        internal var nyVurderingBakgrunn: String? = null

        constructor()

        constructor(innvilgelseBrevbestilling: InnvilgelseBrevbestilling) : super(innvilgelseBrevbestilling) {
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst
            this.ektefelleFritekst = innvilgelseBrevbestilling.ektefelleFritekst
            this.barnFritekst = innvilgelseBrevbestilling.barnFritekst
            this.virksomhetArbeidsgiverSkalHaKopi = innvilgelseBrevbestilling.virksomhetArbeidsgiverSkalHaKopi
            this.nyVurderingBakgrunn = innvilgelseBrevbestilling.nyVurderingBakgrunn
        }

        fun medInnledningFritekst(innledningFritekst: String?) = apply {
            this.innledningFritekst = innledningFritekst
        }

        fun medBegrunnelseFritekst(begrunnelseFritekst: String?) = apply {
            this.begrunnelseFritekst = begrunnelseFritekst
        }

        fun medEktefelleFritekst(ektefelleFritekst: String?) = apply {
            this.ektefelleFritekst = ektefelleFritekst
        }

        fun medBarnFritekst(barnFritekst: String?) = apply {
            this.barnFritekst = barnFritekst
        }

        fun medVirksomhetArbeidsgiverSkalHaKopi(virksomhetArbeidsgiverSkalHaKopi: Boolean) = apply {
            this.virksomhetArbeidsgiverSkalHaKopi = virksomhetArbeidsgiverSkalHaKopi
        }

        fun medNyVurderingBakgrunn(nyVurderingBakgrunn: String?) = apply {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn
        }

        override fun build(): InnvilgelseBrevbestilling {
            return InnvilgelseBrevbestilling(this)
        }
    }
}
