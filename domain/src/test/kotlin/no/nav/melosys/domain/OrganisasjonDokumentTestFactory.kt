package no.nav.melosys.domain

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer

object OrganisasjonDokumentTestFactory {
    const val ORGNUMMER = "123456789"
    const val NAVN = "Ola Norman"
    const val SEKTORKODE = "Ola Norman"

    @JvmStatic
    fun createOrganisasjonDokumentForTest(): OrganisasjonDokument {
        return createOrganisasjonDokumentForTest(ORGNUMMER, NAVN, SEKTORKODE)
    }

    @JvmStatic
    fun createOrganisasjonDokumentForTest(
        orgnummer: String = ORGNUMMER,
        navn: String = NAVN,
        sektorkode: String = SEKTORKODE,
    ): OrganisasjonDokument {
        return OrganisasjonDokument(
            orgnummer = orgnummer,
            navn = navn,
            sektorkode = sektorkode,
            organisasjonDetaljer = OrganisasjonsDetaljer(orgnummer = orgnummer),
        )
    }
}
