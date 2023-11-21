package no.nav.melosys.service;

import java.util.*;

import no.nav.melosys.domain.OrganisasjonDokumentTestFactory;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaksopplysningStubs {

    public static Saksopplysning lagArbeidsforholdOpplysning(List<String> registrereArbeidsgiverOrgnumre) {
        ArbeidsforholdDokument arbeidsforholdDokument = mock(ArbeidsforholdDokument.class);
        when(arbeidsforholdDokument.hentOrgnumre()).thenReturn(new HashSet<>(registrereArbeidsgiverOrgnumre));
        Saksopplysning arbeidsforhold = new Saksopplysning();
        arbeidsforhold.setDokument(arbeidsforholdDokument);
        arbeidsforhold.setType(SaksopplysningType.ARBFORH);
        return arbeidsforhold;
    }

    public static Set<Saksopplysning> lagArbeidsforholdOpplysninger(List<String> registrerteArbeidsgivere) {
        Saksopplysning arbeidsforhold = lagArbeidsforholdOpplysning(registrerteArbeidsgivere);
        return new HashSet<>(Collections.singletonList(arbeidsforhold));
    }

    public static Set<OrganisasjonDokument> lagOrganisasjonDokumenter(Collection<String> organisasjonsnumre) {
        Set<OrganisasjonDokument> organisasjonDokumenter = new HashSet<>();
        for (String orgnummer : organisasjonsnumre) {
            organisasjonDokumenter.add(OrganisasjonDokumentTestFactory.builder()
                .orgnummer(orgnummer)
                .navn("Test:" + orgnummer).build());
        }
        return organisasjonDokumenter;
    }
}
