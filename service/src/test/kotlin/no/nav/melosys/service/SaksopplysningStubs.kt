package no.nav.melosys.service

import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

object SaksopplysningStubs {

    fun lagArbeidsforholdOpplysning(registrereArbeidsgiverOrgnumre: List<String>): Saksopplysning {
        val arbeidsforholdDokument = mock(ArbeidsforholdDokument::class.java)
        `when`(arbeidsforholdDokument.hentOrgnumre()).thenReturn(HashSet(registrereArbeidsgiverOrgnumre))
        return Saksopplysning().apply {
            dokument = arbeidsforholdDokument
            type = SaksopplysningType.ARBFORH
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