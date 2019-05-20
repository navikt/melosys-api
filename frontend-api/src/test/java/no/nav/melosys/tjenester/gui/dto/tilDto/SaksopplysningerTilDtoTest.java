package no.nav.melosys.tjenester.gui.dto.tilDto;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static no.nav.melosys.domain.SaksopplysningType.PERSHIST;
import static no.nav.melosys.domain.SaksopplysningType.PERSOPL;
import static no.nav.melosys.tjenester.gui.dto.tilDto.SaksopplysningerTilDto.medlemsperiodeKomparator;
import static org.assertj.core.api.Assertions.assertThat;

public class SaksopplysningerTilDtoTest {

    private DokumentFactory dokumentFactory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        dokumentFactory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

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

        SaksopplysningerTilDto.ArbeidsforholdComparator arbeidsforholdComparator = new SaksopplysningerTilDto.ArbeidsforholdComparator();
        arbeidsforholdListe.sort(arbeidsforholdComparator);
        assertThat(arbeidsforholdListe.get(0)).isEqualTo(a3);
        assertThat(arbeidsforholdListe.get(arbeidsforholdListe.size() - 1)).isEqualTo(a1);
    }

    @Test
    public void testMedlemsperioderKronologisk(){
        List<Medlemsperiode> medlemsperioder = new ArrayList<>() ;
        Medlemsperiode medlemsperiode1 = new Medlemsperiode();
        medlemsperiode1.type = "PMMEDSKP";
        medlemsperiode1.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2005,1,1),LocalDate.of(2006,5,30));

        Medlemsperiode medlemsperiode2 = new Medlemsperiode();
        medlemsperiode2.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2016,1,1),LocalDate.of(2016,12,31));
        medlemsperiode2.type = "PUMEDSKP";

        Medlemsperiode medlemsperiode3 = new Medlemsperiode();
        medlemsperiode3.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2017,1,1),LocalDate.of(2017,12,31));
        medlemsperiode3.type = "PUMEDSKP";

        Medlemsperiode medlemsperiode4 = new Medlemsperiode();
        medlemsperiode4.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2018,1,1),LocalDate.of(2018,12,31));
        medlemsperiode4.type = "PMMEDSKP";

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

    @Test
    public void testKonverteringPersonMedStatsborgerskap() throws Exception {

        Saksopplysning personDokument = lagDokument("88888888882.xml", PERSOPL, "3.0");
        Saksopplysning personhistorikkDokument = lagDokument("88888888882_historikk.xml", PERSHIST, "3.4");

        assertThat(personDokument).isNotNull();
        assertThat(personhistorikkDokument).isNotNull();

        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        saksopplysninger.add(personDokument);
        saksopplysninger.add(personhistorikkDokument);

        Behandling behandling = new Behandling();
        behandling.setSisteOpplysningerHentetDato(LocalDate.of(2018, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        behandling.setSaksopplysninger(saksopplysninger);

        SaksopplysningerDto saksopplysningerDto = new SaksopplysningerTilDto().getSaksopplysningerDto(saksopplysninger, behandling);

        PersonDokument person = saksopplysningerDto.getPerson();

        assertThat(person).isNotNull();
        assertThat(person.statsborgerskap).isNotNull();
        assertThat(person.statsborgerskapDato).isNotNull();
    }

    private Saksopplysning lagDokument(String ressurs, SaksopplysningType type, String versjon) {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, Charset.forName("UTF-8")))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(type);
            saksopplysning.setVersjon(versjon);

            dokumentFactory.lagDokument(saksopplysning);

            return saksopplysning;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}