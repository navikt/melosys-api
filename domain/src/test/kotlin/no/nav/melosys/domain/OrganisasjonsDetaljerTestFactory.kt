package no.nav.melosys.domain

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer

object OrganisasjonsDetaljerTestFactory {
    const val ORGNUMMER = "123456789"

    @JvmStatic
    fun createOrganisasjonsDetaljerForTest(
    ): OrganisasjonsDetaljer = createOrganisasjonsDetaljerForTest(
        orgnummer = ORGNUMMER
    )

    @JvmStatic
    fun createOrganisasjonsDetaljerForTest(
        orgnummer: String = ORGNUMMER
    ): OrganisasjonsDetaljer {
        return OrganisasjonsDetaljer(orgnummer = orgnummer)
    }
}
