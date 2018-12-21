package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.dokument.felles.Periode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArbeidsforholdDokumentTest {
    private ArbeidsforholdDokument arbeidsforholdDokument;
    private List<Periode> perioderIDokument = new ArrayList<>();

    private final String orgNr1 = "12345678910";
    private final String orgNr2 = "10987654321";

    public ArbeidsforholdDokumentTest() {
        arbeidsforholdDokument = new ArbeidsforholdDokument();

        leggTilArbeidsforhold(orgNr1, new Periode(LocalDate.now(), LocalDate.now()));
    }

    public Arbeidsforhold leggTilArbeidsforhold(String orgNummer, Periode periode) {
        perioderIDokument.add(periode);

        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = orgNummer;
        arbeidsforhold.ansettelsesPeriode = periode;
        arbeidsforholdDokument.arbeidsforhold.add(arbeidsforhold);
        return arbeidsforhold;
    }

    @Test
    public void hentAnsettelsesperioderIngenValgteOrgnummer() {
        List<String> tomListeMedOrgnumre = new ArrayList<>();
        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(tomListeMedOrgnumre);
        assertThat(perioder).isEmpty();
    }

    @Test
    public void hentAnsettelsesperioderKunUtvalgteOrgnumre() {
        Periode forventetPeriode = perioderIDokument.get(0);
        leggTilArbeidsforhold(orgNr2, new Periode(LocalDate.now(), LocalDate.now()));

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr1);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).containsOnly(forventetPeriode);
    }

    @Test
    public void hentAnsettelsesperioder() {
        leggTilArbeidsforhold(orgNr2, new Periode(LocalDate.now(), LocalDate.now()));

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr1);
        orgnumre.add(orgNr2);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).containsAll(perioderIDokument);
    }

    @Test
    public void hentAnsettelsesperioderFiltrererUdefinertePerioder() {
        Periode forventetPeriode = perioderIDokument.get(0);
        leggTilArbeidsforhold(orgNr2, null);

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr1);
        orgnumre.add(orgNr2);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).containsOnly(forventetPeriode);
    }

    @Test
    public void hentAlleOrgnumre() {
        Arbeidsforhold arbeidsforhold =
                leggTilArbeidsforhold(orgNr2, new Periode(LocalDate.now(), LocalDate.now()));

        arbeidsforhold.opplysningspliktigID = "123123123";
        Set<String> orgnumre = arbeidsforholdDokument.hentOrgnumre();
        assertThat(orgnumre).containsExactlyInAnyOrder(orgNr1, orgNr2, arbeidsforhold.opplysningspliktigID);
    }
}
