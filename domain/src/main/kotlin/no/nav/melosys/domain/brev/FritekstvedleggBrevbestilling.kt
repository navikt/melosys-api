package no.nav.melosys.domain.brev

import no.nav.melosys.domain.kodeverk.Mottakerroller

class FritekstvedleggBrevbestilling : DokgenBrevbestilling {
    var fritekstvedleggTittel: String? = null
    var fritekstvedleggTekst: String? = null
    var mottakerType: Mottakerroller? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fritekstvedleggTittel = builder.fritekstvedleggTittel
        this.fritekstvedleggTekst = builder.fritekstvedleggTekst
        this.mottakerType = builder.mottakerType
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var fritekstvedleggTittel: String? = null
        internal var fritekstvedleggTekst: String? = null
        internal var mottakerType: Mottakerroller? = null

        constructor()

        constructor(fritekstvedleggBrevbestilling: FritekstvedleggBrevbestilling) : super(fritekstvedleggBrevbestilling) {
            this.fritekstvedleggTittel = fritekstvedleggBrevbestilling.fritekstvedleggTittel
            this.fritekstvedleggTekst = fritekstvedleggBrevbestilling.fritekstvedleggTekst
            this.mottakerType = fritekstvedleggBrevbestilling.mottakerType
        }

        fun medFritekstvedleggTittel(fritekstvedleggTittel: String?) = apply { this.fritekstvedleggTittel = fritekstvedleggTittel }

        fun medFritekstvedleggTekst(fritekstvedleggTekst: String?) = apply { this.fritekstvedleggTekst = fritekstvedleggTekst }

        fun medMottakerType(mottakerType: Mottakerroller?) = apply { this.mottakerType = mottakerType }

        override fun build(): FritekstvedleggBrevbestilling = FritekstvedleggBrevbestilling(this)
    }
}
