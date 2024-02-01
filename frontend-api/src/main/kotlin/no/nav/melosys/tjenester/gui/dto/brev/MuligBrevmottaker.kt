package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller

@JvmRecord
data class MuligBrevmottaker(
    val mottakerNavn: String,
    val dokumentNavn: String,
    val rolle: Mottakerroller,
    val orgnr: String,
    val aktørId: String,
    val institusjonID: String
) {
    companion object {
        @JvmStatic
        fun av(hovedMottaker: Brevmottaker): MuligBrevmottaker {
            return MuligBrevmottaker(
                hovedMottaker.mottakerNavn,
                hovedMottaker.dokumentNavn,
                hovedMottaker.rolle,
                hovedMottaker.orgnr,
                hovedMottaker.aktørId,
                hovedMottaker.institusjonID
            )
        }
    }
}
