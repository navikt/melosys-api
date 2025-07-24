package no.nav.melosys.domain.brev

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.melosys.domain.kodeverk.Mottakerroller

class FritekstbrevBrevbestilling : DokgenBrevbestilling {
    var fritekstTittel: String? = null
    var fritekst: String? = null

    @JsonAlias("isKontaktopplysninger")
    var kontaktopplysninger: Boolean = false

    var navnFullmektig: String? = null
    var saksbehandlerNrToNavn: String? = null

    @JsonAlias("isBrukerSkalHaKopi")
    var brukerSkalHaKopi: Boolean = false

    var mottakerType: Mottakerroller? = null
    var dokumentTittel: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fritekstTittel = builder.fritekstTittel
        this.fritekst = builder.fritekst
        this.kontaktopplysninger = builder.kontaktopplysninger
        this.navnFullmektig = builder.navnFullmektig
        this.saksbehandlerNrToNavn = builder.saksbehandlerNrToNavn
        this.brukerSkalHaKopi = builder.brukerSkalHaKopi
        this.mottakerType = builder.mottakerType
        this.dokumentTittel = builder.dokumentTittel
    }

    // Java compatibility methods
    fun isKontaktopplysninger(): Boolean = kontaktopplysninger
    fun isBrukerSkalHaKopi(): Boolean = brukerSkalHaKopi

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var fritekstTittel: String? = null
        internal var fritekst: String? = null
        internal var kontaktopplysninger: Boolean = false
        internal var navnFullmektig: String? = null
        internal var saksbehandlerNrToNavn: String? = null
        internal var brukerSkalHaKopi: Boolean = false
        internal var mottakerType: Mottakerroller? = null
        internal var dokumentTittel: String? = null

        constructor()

        constructor(fritekstbrevBrevbestilling: FritekstbrevBrevbestilling) : super(fritekstbrevBrevbestilling) {
            this.fritekstTittel = fritekstbrevBrevbestilling.fritekstTittel
            this.fritekst = fritekstbrevBrevbestilling.fritekst
            this.kontaktopplysninger = fritekstbrevBrevbestilling.kontaktopplysninger
            this.navnFullmektig = fritekstbrevBrevbestilling.navnFullmektig
            this.saksbehandlerNrToNavn = fritekstbrevBrevbestilling.saksbehandlerNrToNavn
            this.brukerSkalHaKopi = fritekstbrevBrevbestilling.brukerSkalHaKopi
            this.mottakerType = fritekstbrevBrevbestilling.mottakerType
            this.dokumentTittel = fritekstbrevBrevbestilling.dokumentTittel
        }

        fun medFritekstTittel(fritekstTittel: String?) = apply { this.fritekstTittel = fritekstTittel }

        fun medFritekst(fritekst: String?) = apply { this.fritekst = fritekst }

        fun medKontaktopplysninger(kontaktopplysninger: Boolean) = apply { this.kontaktopplysninger = kontaktopplysninger }

        fun medNavnFullmektig(navnFullmektig: String?) = apply { this.navnFullmektig = navnFullmektig }

        fun medSaksbehandlerNrToNavn(saksbehandlerNrToNavn: String?) = apply { this.saksbehandlerNrToNavn = saksbehandlerNrToNavn }

        fun medBrukerSkalHaKopi(brukerSkalHaKopi: Boolean) = apply { this.brukerSkalHaKopi = brukerSkalHaKopi }

        fun medDokumentTittel(dokumentTittel: String?) = apply { this.dokumentTittel = dokumentTittel }

        fun medMottakerType(mottakerType: Mottakerroller?) = apply { this.mottakerType = mottakerType }

        override fun build(): FritekstbrevBrevbestilling = FritekstbrevBrevbestilling(this)
    }
}
