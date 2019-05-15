package no.nav.melosys.service;

import java.util.*;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

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

    public static Saksopplysning lagSøknadOpplysning(List<String> selvstendigeForetak, List<String> ekstraArbeidsgivere) {
        SoeknadDokument søknad = new SoeknadDokument();
        for (String orgnr : selvstendigeForetak) {
            SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
            selvstendigForetak.orgnr = orgnr;
            søknad.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        }

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landkode = "DE";
        søknad.arbeidUtland = new ArrayList<>();
        søknad.arbeidUtland.add(arbeidUtland);
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.addAll(ekstraArbeidsgivere);
        søknad.soeknadsland.landkoder.add("DE");

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(søknad);
        saksopplysning.setType(SaksopplysningType.SØKNAD);

        return saksopplysning;
    }

    public static Set<Saksopplysning> lagSøknadOgArbeidsforholdOpplysninger(List<String> selvstendigeForetak, List<String> ekstraArbeidsgivere, List<String> registrerteArbeidsgivere) {
        Saksopplysning søknad = lagSøknadOpplysning(selvstendigeForetak, ekstraArbeidsgivere);
        Saksopplysning arbeidsforhold = lagArbeidsforholdOpplysning(registrerteArbeidsgivere);
        return new HashSet<>(Arrays.asList(søknad, arbeidsforhold));
    }

    public static Set<OrganisasjonDokument> lagOrganisasjonDokumenter(Collection<String> organisasjonsnumre)  {
        Set<OrganisasjonDokument> organisasjonDokumenter = new HashSet<>();
        for (String orgnummer : organisasjonsnumre) {
            OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
            organisasjonDokument.setOrgnummer(orgnummer);
            organisasjonDokument.setNavn(Arrays.asList("Test:", orgnummer));
            organisasjonDokument.setOrganisasjonDetaljer(new OrganisasjonsDetaljer());
            organisasjonDokumenter.add(organisasjonDokument);
        }
        return organisasjonDokumenter;
    }
}
