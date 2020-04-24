package no.nav.melosys.service;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.integrasjon.regelmodul.RegelmodulFasade;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.parsers.ParserConfigurationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static no.nav.melosys.domain.dokument.felles.Land.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class RegelmodulServiceTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private RegelmodulFasade regelmodulFasade;

    private RegelmodulService regelmodulService;

    @Before
    public void setUp() throws ParserConfigurationException {
        regelmodulService = new RegelmodulService(saksopplysningerService, regelmodulFasade);
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_tomListe_girNull() {
        Land stastborgerskap = regelmodulService.avgjørStatsborgerskapPåStartDato(new ArrayList<>(), null);
        assertNull(stastborgerskap);
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_ingenGyldige_girNull() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2018, 4, 1), LocalDate.of(2018, 5, 2));
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = regelmodulService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2019, 2, 1));
        assertNull(stastborgerskap);
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_flerePerioder_girPeriodenSomInkludererStartdato() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2018, 4, 1), null);
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = regelmodulService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(BELGIA));
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_flerePerioder_filtererSkd() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        p1.endretAv = "NAV";
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2017, 4, 1), null);
        p2.endretAv = "SKD";
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = regelmodulService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(BELGIA));
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_flereGyldige_filtrererUkjent() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        p1.endretAv = "NAV";
        p1.endringstidspunkt = LocalDateTime.now().minusYears(3);
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2017, 4, 1), null);
        p2.endretAv = "NAV";
        p2.endringstidspunkt = LocalDateTime.now().minusYears(2);
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = regelmodulService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(BELGIA));
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_flereGyldige_girSistEndret() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        p1.endretAv = "NAV";
        p1.endringstidspunkt = LocalDateTime.now().minusYears(3);
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(SVERIGE);
        p2.periode = new Periode(LocalDate.of(2017, 4, 1), null);
        p2.endretAv = "NAV";
        p2.endringstidspunkt = LocalDateTime.now().minusYears(2);
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = regelmodulService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(SVERIGE));
    }
}