package no.nav.melosys.tjenester.gui.dto.converter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SaksopplysningerTilDtoConverterTest {

    @Test
    public void testArbeidsforholdSortering() {
        List<Arbeidsforhold> arbeidsforholdListe = new ArrayList<>();
        Arbeidsforhold a1 = new Arbeidsforhold();
        a1.ansettelsesPeriode = new Periode(LocalDate.now(), LocalDate.MAX);
        arbeidsforholdListe.add(a1);
        Arbeidsforhold a2 = new Arbeidsforhold();
        a2.ansettelsesPeriode = new Periode(LocalDate.now(), null);
        arbeidsforholdListe.add(a2);
        Arbeidsforhold a3 = new Arbeidsforhold();
        a3.ansettelsesPeriode = new Periode(LocalDate.now().plusYears(1), null);
        arbeidsforholdListe.add(a3);
        Arbeidsforhold a4 = new Arbeidsforhold();
        a4.ansettelsesPeriode = new Periode(LocalDate.now().plusYears(2), LocalDate.MAX);
        arbeidsforholdListe.add(a4);

        SaksopplysningerTilDtoConverter.ArbeidsforholdComparator arbeidsforholdComparator = new SaksopplysningerTilDtoConverter.ArbeidsforholdComparator();
        arbeidsforholdListe.sort(arbeidsforholdComparator);
        assertThat(arbeidsforholdListe.get(0)).isEqualTo(a3);
        assertThat(arbeidsforholdListe.get(arbeidsforholdListe.size() - 1)).isEqualTo(a1);
    }

    @Test
    public void testMedlemskapPerioderKronolgisk(){
        List<Medlemsperiode> medlemsperiodes = new ArrayList<>() ;
        Medlemsperiode medlemsperiode1 = new Medlemsperiode();
        medlemsperiode1.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2005,01,01),LocalDate.of(2006,05,30));

        Medlemsperiode medlemsperiode2 = new Medlemsperiode();
        medlemsperiode2.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2016,01,01),LocalDate.of(2016,12,31));

        Medlemsperiode medlemsperiode3 = new Medlemsperiode();
        medlemsperiode3.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2017,01,01),LocalDate.of(2017,12,31));

        medlemsperiodes.add(medlemsperiode1);
        medlemsperiodes.add(medlemsperiode2);
        medlemsperiodes.add(medlemsperiode3);

        SaksopplysningerTilDtoConverter.MedlemsPeriodComparator medlemsPeriodComparator = new SaksopplysningerTilDtoConverter.MedlemsPeriodComparator();
        medlemsperiodes.sort(medlemsPeriodComparator);
        assertThat(medlemsperiodes.get(0)).isEqualTo(medlemsperiode3);
        assertThat(medlemsperiodes.get(1)).isEqualTo(medlemsperiode2);
        assertThat(medlemsperiodes.get(2)).isEqualTo(medlemsperiode1);
    }
}