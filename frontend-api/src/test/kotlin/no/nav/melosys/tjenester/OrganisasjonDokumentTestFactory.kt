package no.nav.melosys.tjenester

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument

object OrganisasjonDokumentTestFactory {
    @JvmStatic
    fun createOrganisasjonDokumentForTest(
    ): OrganisasjonDokument = createOrganisasjonDokumentForTest("123456789", "Ola Norman")

    @JvmStatic
    fun createOrganisasjonDokumentForTest(orgnummer: String = "123456789", navn: String = "Ola Norman"): OrganisasjonDokument {
        return OrganisasjonDokument(
            orgnummer = orgnummer,
            navn = navn
        )
    }
}
