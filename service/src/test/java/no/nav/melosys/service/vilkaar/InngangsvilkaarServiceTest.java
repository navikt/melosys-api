package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.dokument.soeknad.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.regelmodul.RegelmodulFasade;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.service.SaksopplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.dokument.felles.Land.*;
import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class InngangsvilkaarServiceTest {
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private RegelmodulFasade regelmodulFasade;
    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    private InngangsvilkaarService inngangsvilkaarService;

    @Before
    public void setUp() {
        inngangsvilkaarService = new InngangsvilkaarService(saksopplysningerService, regelmodulFasade, vilkaarsresultatService);
    }

    @Test
    public void vurderOgLagreInngangsvilkår() throws TekniskException, FunksjonellException {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.dokument.soeknad.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        PersonDokument personDokument = new PersonDokument();
        personDokument.statsborgerskap = Land.av(FINLAND);
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.emptyList();
        res.kvalifisererForEf883_2004 = true;
        when(regelmodulFasade.vurderInngangsvilkår(any(), anyList(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, Soeknadsland.av(landkoder), periode);

        verify(regelmodulFasade).vurderInngangsvilkår(eq(personDokument.statsborgerskap), eq(tilIso3(landkoder)), eq(periode));
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR, true, null);
    }

    @Test
    public void avgjørStatsborgerskapPåStartDato_tomListe_girNull() {
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(new ArrayList<>(), null);
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
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2019, 2, 1));
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
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
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
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
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
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
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
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(SVERIGE));
    }
}