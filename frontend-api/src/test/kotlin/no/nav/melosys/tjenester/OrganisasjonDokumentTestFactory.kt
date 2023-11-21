package no.nav.melosys.tjenester

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer

object OrganisasjonDokumentTestFactory {
    @JvmStatic
    fun createOrganisasjonDokumentForTest(
    ): OrganisasjonDokument = createOrganisasjonDokumentForTest(
        "123456789",
        "Ola Norman",
        "")

    @JvmStatic
    fun createOrganisasjonDokumentForTest(
        orgnummer: String = "123456789",
        navn: String = "Ola Norman",
        sektorkode: String = "",
    ): OrganisasjonDokument {
        return OrganisasjonDokument(
            orgnummer = orgnummer,
            navn = navn,
            sektorkode = sektorkode,
            organisasjonDetaljer = OrganisasjonsDetaljer(orgnummer = orgnummer),
        )
    }
}

object OrganisasjonsDetaljerTestFactory {
    @JvmStatic
    fun createOrganisasjonsDetaljerForTest(
    ): OrganisasjonsDetaljer = createOrganisasjonsDetaljerForTest(
        "123456789"
    )

    @JvmStatic
    fun createOrganisasjonsDetaljerForTest(
        orgnummer: String = "123456789"
    ): OrganisasjonsDetaljer {
        return OrganisasjonsDetaljer(orgnummer = orgnummer)
    }
}

