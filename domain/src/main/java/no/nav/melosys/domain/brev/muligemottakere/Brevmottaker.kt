package no.nav.melosys.domain.brev.muligemottakere

import no.nav.melosys.domain.kodeverk.Mottakerroller

data class Brevmottaker(
    val mottakerNavn: String? = null,
    val dokumentNavn: String? = null,
    val rolle: Mottakerroller? = null,
    val orgnr: String? = null,
    val aktørId: String? = null,
    val institusjonID: String? = null
) {

    class Builder {
        private var mottakerNavn: String? = null
        private var dokumentNavn: String? = null
        private var rolle: Mottakerroller? = null
        private var orgnr: String? = null
        private var aktørId: String? = null
        private var institusjonID: String? = null

        fun medMottakerNavn(mottakerNavn: String?) = apply { this.mottakerNavn = mottakerNavn }

        fun medDokumentNavn(dokumentNavn: String?) = apply { this.dokumentNavn = dokumentNavn }

        fun medRolle(rolle: Mottakerroller?) = apply { this.rolle = rolle }

        fun medOrgnr(orgnr: String?) = apply { this.orgnr = orgnr }

        fun medAktørId(aktørId: String?) = apply { this.aktørId = aktørId }

        fun medInstitusjonID(institusjonID: String?) = apply { this.institusjonID = institusjonID }

        fun build() = Brevmottaker(
            mottakerNavn = mottakerNavn,
            dokumentNavn = dokumentNavn,
            rolle = rolle,
            orgnr = orgnr,
            aktørId = aktørId,
            institusjonID = institusjonID
        )
    }
}
