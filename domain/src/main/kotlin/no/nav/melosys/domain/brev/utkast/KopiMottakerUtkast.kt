package no.nav.melosys.domain.brev.utkast

import no.nav.melosys.domain.kodeverk.Mottakerroller

@JvmRecord
data class KopiMottakerUtkast(
    val rolle: Mottakerroller?,
    val orgnr: String?,
    val aktørID: String?,
    val institusjonID: String?
)

