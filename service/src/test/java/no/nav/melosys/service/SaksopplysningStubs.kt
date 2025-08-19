package no.nav.melosys.service

import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

object SaksopplysningStubs {

    @JvmStatic
    fun lagArbeidsforholdOpplysning(registrereArbeidsgiverOrgnumre: List<String>): Saksopplysning {
        val arbeidsforholdDokument = mock(ArbeidsforholdDokument::class.java)
        `when`(arbeidsforholdDokument.hentOrgnumre()).thenReturn(HashSet(registrereArbeidsgiverOrgnumre))
        val arbeidsforhold = Saksopplysning()
        arbeidsforhold.dokument = arbeidsforholdDokument
        arbeidsforhold.type = SaksopplysningType.ARBFORH
        return arbeidsforhold
    }

    @JvmStatic
    fun lagArbeidsforholdOpplysninger(registrerteArbeidsgivere: List<String>): MutableSet<Saksopplysning> {
        val arbeidsforhold = lagArbeidsforholdOpplysning(registrerteArbeidsgivere)
        return mutableSetOf(arbeidsforhold)
    }

    @JvmStatic
    fun lagOrganisasjonDokumenter(organisasjonsnumre: Collection<String>): Set<OrganisasjonDokument> {
        val organisasjonDokumenter = hashSetOf<OrganisasjonDokument>()
        for (orgnummer in organisasjonsnumre) {
            organisasjonDokumenter.add(
                OrganisasjonDokumentTestFactory.builder()
                    .orgnummer(orgnummer)
                    .navn("Test:$orgnummer").build()
            )
        }
        return organisasjonDokumenter
    }
}