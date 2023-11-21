package no.nav.melosys.domain

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse

object OrganisasjonsDetaljerTestFactory {
    const val ORGNUMMER = "123456789"

    @JvmStatic
    fun builder() = Builder()
    class Builder(
        private var orgnummer: String? = null,
        private var forretningsadresse: List<GeografiskAdresse> = emptyList(),
        private var postadresse: List<GeografiskAdresse> = emptyList(),
    ) {
        fun orgnummer(orgnummer: String) = apply { this.orgnummer = orgnummer }
        fun forretningsadresser(forretningsadresser: List<GeografiskAdresse>) = apply { this.forretningsadresse = forretningsadresser }
        fun forretningsadresse(forretningsadresse: GeografiskAdresse) = forretningsadresser(listOf(forretningsadresse))
        fun postadresser(postadresser: List<GeografiskAdresse>) = apply { this.postadresse = postadresser }
        fun postadresse(postadresse: GeografiskAdresse) = postadresser(listOf(postadresse))

        fun build() = OrganisasjonsDetaljer(
            orgnummer = orgnummer ?: ORGNUMMER,
            forretningsadresse = forretningsadresse,
            postadresse = postadresse,
        )
    }
}
