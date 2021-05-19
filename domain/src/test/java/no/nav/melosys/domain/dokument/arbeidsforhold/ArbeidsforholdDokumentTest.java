package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.dokument.felles.Periode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArbeidsforholdDokumentTest {
    private ArbeidsforholdDokument arbeidsforholdDokument;
    private Periode eksisterendePeriode;

    private final String orgNr1 = "12345678910";
    private final String orgNr2 = "10987654321";

    public ArbeidsforholdDokumentTest() {
        arbeidsforholdDokument = new ArbeidsforholdDokument();
        eksisterendePeriode = new Periode(LocalDate.now(), LocalDate.now());
        leggTilArbeidsforhold(orgNr1, null, eksisterendePeriode);
    }

    public Arbeidsforhold leggTilArbeidsforhold(String arbeidsgiverID, String opplysningspliktigID, Periode periode) {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = arbeidsgiverID;
        arbeidsforhold.opplysningspliktigID = opplysningspliktigID;
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
        leggTilArbeidsforhold(orgNr2, null, new Periode(LocalDate.now(), LocalDate.now()));

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr1);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).containsOnly(eksisterendePeriode);
    }

    @Test
    public void hentAnsettelsesperioderOpplysningspliktigOrgnumre() {
        Periode forventetPeriode = new Periode(LocalDate.now().minusYears(1), LocalDate.now());
        leggTilArbeidsforhold(null, orgNr2, forventetPeriode);

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr2);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).containsOnly(forventetPeriode);
    }

    @Test
    public void hentAnsettelsesperioder() {
        Periode nyPeriode = new Periode(LocalDate.now(), LocalDate.now());
        leggTilArbeidsforhold(orgNr2, null, nyPeriode);

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr1);
        orgnumre.add(orgNr2);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).contains(eksisterendePeriode, nyPeriode);
    }

    @Test
    public void hentAnsettelsesperioderFiltrererUdefinertePerioder() {
        leggTilArbeidsforhold(orgNr2, null, null);

        List<String> orgnumre = new ArrayList<>();
        orgnumre.add(orgNr1);
        orgnumre.add(orgNr2);

        Set<Periode> perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre);
        assertThat(perioder).hasSize(1);
        assertThat(perioder).containsOnly(eksisterendePeriode);
    }

    @Test
    public void hentAlleOrgnumre() {
        String orgNr3 = "123123123";
        leggTilArbeidsforhold(orgNr2, orgNr3, new Periode(LocalDate.now(), LocalDate.now()));

        Set<String> orgnumre = arbeidsforholdDokument.hentOrgnumre();
        assertThat(orgnumre).containsExactlyInAnyOrder(orgNr1, orgNr2, orgNr3);
    }
}
