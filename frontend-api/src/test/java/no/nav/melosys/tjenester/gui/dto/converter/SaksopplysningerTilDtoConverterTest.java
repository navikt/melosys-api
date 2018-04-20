package no.nav.melosys.tjenester.gui.dto.converter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periodetype;
import org.junit.Test;

import static no.nav.melosys.tjenester.gui.dto.converter.SaksopplysningerTilDtoConverter.medlemsperiodeKomparator;
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
    public void testMedlemsperioderKronologisk(){
        List<Medlemsperiode> medlemsperioder = new ArrayList<>() ;
        Medlemsperiode medlemsperiode1 = new Medlemsperiode();
        medlemsperiode1.type = Periodetype.PMMEDSKP;
        medlemsperiode1.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2005,1,1),LocalDate.of(2006,5,30));

        Medlemsperiode medlemsperiode2 = new Medlemsperiode();
        medlemsperiode2.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2016,1,1),LocalDate.of(2016,12,31));
        medlemsperiode2.type = Periodetype.PUMEDSKP;

        Medlemsperiode medlemsperiode3 = new Medlemsperiode();
        medlemsperiode3.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2017,1,1),LocalDate.of(2017,12,31));
        medlemsperiode3.type = Periodetype.PUMEDSKP;

        Medlemsperiode medlemsperiode4 = new Medlemsperiode();
        medlemsperiode4.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2018,1,1),LocalDate.of(2018,12,31));
        medlemsperiode4.type = Periodetype.PMMEDSKP;

        medlemsperioder.add(medlemsperiode1);
        medlemsperioder.add(medlemsperiode2);
        medlemsperioder.add(medlemsperiode3);
        medlemsperioder.add(medlemsperiode4);

        medlemsperioder.sort(Comparator.comparing(Medlemsperiode::getType).thenComparing(medlemsperiodeKomparator));

        assertThat(medlemsperioder.get(0)).isEqualTo(medlemsperiode4);
        assertThat(medlemsperioder.get(1)).isEqualTo(medlemsperiode1);
        assertThat(medlemsperioder.get(2)).isEqualTo(medlemsperiode3);
        assertThat(medlemsperioder.get(3)).isEqualTo(medlemsperiode2);
    }
}