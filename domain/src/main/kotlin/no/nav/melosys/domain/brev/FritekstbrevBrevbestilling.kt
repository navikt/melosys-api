package no.nav.melosys.domain.brev

import no.nav.melosys.domain.kodeverk.Mottakerroller

class FritekstbrevBrevbestilling : DokgenBrevbestilling {
    var fritekstTittel: String? = null
        private set
    var fritekst: String? = null
        private set
    var isKontaktopplysninger: Boolean = false
        private set
    var navnFullmektig: String? = null
        private set
    var saksbehandlerNrToNavn: String? = null
        private set
    var isBrukerSkalHaKopi: Boolean = false
        private set
    var mottakerType: Mottakerroller? = null
        private set
    var dokumentTittel: String? = null
        private set

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fritekstTittel = builder.fritekstTittel
        this.fritekst = builder.fritekst
        this.isKontaktopplysninger = builder.kontaktopplysninger
        this.navnFullmektig = builder.navnFullmektig
        this.saksbehandlerNrToNavn = builder.saksbehandlerNrToNavn
        this.isBrukerSkalHaKopi = builder.brukerSkalHaKopi
        this.mottakerType = builder.mottakerType
        this.dokumentTittel = builder.dokumentTittel
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder?> {
        internal var fritekstTittel: String? = null
        var fritekst: String? = null
        var kontaktopplysninger: Boolean = false
        var navnFullmektig: String? = null
        var saksbehandlerNrToNavn: String? = null
        var brukerSkalHaKopi: Boolean = false
        var mottakerType: Mottakerroller? = null
        var dokumentTittel: String? = null

        constructor()

        constructor(fritekstbrevBrevbestilling: FritekstbrevBrevbestilling) : super(fritekstbrevBrevbestilling) {
            this.fritekstTittel = fritekstbrevBrevbestilling.fritekstTittel
            this.fritekst = fritekstbrevBrevbestilling.fritekst
            this.kontaktopplysninger = fritekstbrevBrevbestilling.isKontaktopplysninger
            this.navnFullmektig = fritekstbrevBrevbestilling.navnFullmektig
            this.saksbehandlerNrToNavn = fritekstbrevBrevbestilling.saksbehandlerNrToNavn
            this.brukerSkalHaKopi = fritekstbrevBrevbestilling.isBrukerSkalHaKopi
            this.mottakerType = fritekstbrevBrevbestilling.mottakerType
            this.dokumentTittel = fritekstbrevBrevbestilling.dokumentTittel
        }

        fun medFritekstTittel(fritekstTittel: String?): Builder {
            this.fritekstTittel = fritekstTittel
            return this
        }

        fun medFritekst(fritekst: String?): Builder {
            this.fritekst = fritekst
            return this
        }

        fun medKontaktopplysninger(kontaktopplysninger: Boolean): Builder {
            this.kontaktopplysninger = kontaktopplysninger
            return this
        }

        fun medNavnFullmektig(navnFullmektig: String?): Builder {
            this.navnFullmektig = navnFullmektig
            return this
        }

        fun medSaksbehandlerNrToNavn(saksbehandlerNrToNavn: String?): Builder {
            this.saksbehandlerNrToNavn = saksbehandlerNrToNavn
            return this
        }

        fun medBrukerSkalHaKopi(brukerSkalHaKopi: Boolean): Builder {
            this.brukerSkalHaKopi = brukerSkalHaKopi
            return this
        }

        fun medDokumentTittel(dokumentTittel: String?): Builder {
            this.dokumentTittel = dokumentTittel
            return this
        }

        fun medMottakerType(mottakerType: Mottakerroller?): Builder {
            this.mottakerType = mottakerType
            return this
        }

        override fun build(): FritekstbrevBrevbestilling {
            return FritekstbrevBrevbestilling(this)
        }
    }
}
