package no.nav.melosys.domain

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer

object OrganisasjonDokumentTestFactory {
    const val ORGNUMMER = "123456789"
    const val NAVN = "Ola Norman"
    const val SEKTORKODE = "6500"

    @JvmStatic
    fun builder() = Builder()

    class Builder(
        private var orgnummer: String = ORGNUMMER,
        private var navn: String = NAVN,
        private var sektorkode: String = SEKTORKODE,
        private var organisasjonsDetaljer: OrganisasjonsDetaljer? = null
    ) {
        fun orgnummer(orgnummer: String) = apply { this.orgnummer = orgnummer }
        fun navn(navn: String) = apply { this.navn = navn }
        fun sektorkode(sektorkode: String) = apply { this.sektorkode = sektorkode }
        fun organisasjonsDetaljer(organisasjonsDetaljer: OrganisasjonsDetaljer) = apply { this.organisasjonsDetaljer = organisasjonsDetaljer }
        fun build() = OrganisasjonDokument(
            orgnummer = orgnummer,
            navn = navn,
            sektorkode = sektorkode,
            organisasjonDetaljer = organisasjonsDetaljer ?: OrganisasjonsDetaljer(orgnummer = orgnummer)
        )
    }
}
