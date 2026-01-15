package no.nav.melosys.service

import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.arbeidsforholdDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.saksopplysningForTest

object SaksopplysningStubs {

    fun lagArbeidsforholdOpplysning(registrereArbeidsgiverOrgnumre: List<String>): Saksopplysning =
        saksopplysningForTest {
            type = SaksopplysningType.ARBFORH
            arbeidsforholdDokument {
                registrereArbeidsgiverOrgnumre.forEach { orgnr ->
                    arbeidsforhold {
                        arbeidsgiverID = orgnr
                    }
                }
            }
        }

    fun lagArbeidsforholdOpplysninger(registrerteArbeidsgivere: List<String>): MutableSet<Saksopplysning> =
        mutableSetOf(lagArbeidsforholdOpplysning(registrerteArbeidsgivere))

    fun lagOrganisasjonDokumenter(organisasjonsnumre: Collection<String>): Set<OrganisasjonDokument> =
        organisasjonsnumre.map { orgnummer ->
            OrganisasjonDokumentTestFactory.builder()
                .orgnummer(orgnummer)
                .navn("Test:$orgnummer")
                .build()
        }.toHashSet()
}
