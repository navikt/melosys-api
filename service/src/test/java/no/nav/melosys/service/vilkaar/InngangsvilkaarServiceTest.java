package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.inngangsvilkar.Feilmelding;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.domain.inngangsvilkar.Kategori;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumerImpl;
import no.nav.melosys.service.SaksopplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.dokument.felles.Land.*;
import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
public class InngangsvilkaarServiceTest {
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private InngangsvilkaarConsumerImpl inngangsvilkaarConsumer;
    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    private InngangsvilkaarService inngangsvilkaarService;

    @BeforeEach
    public void setUp() {
        inngangsvilkaarService = new InngangsvilkaarService(saksopplysningerService, inngangsvilkaarConsumer, vilkaarsresultatService);
    }

    @Test
    public void vurderOgLagreInngangsvilkår() throws TekniskException, FunksjonellException {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        PersonDokument personDokument = new PersonDokument();
        personDokument.statsborgerskap = Land.av(FINLAND);
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(eq(personDokument.statsborgerskap), eq(Set.copyOf(tilIso3(landkoder))), eq(periode));
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR, true, null);
    }

    @Test
    public void vurderOgLagreInngangsvilkår_manglerStatsborgerskap_girBegrunnelse() throws TekniskException, FunksjonellException {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));
        final var personhistorikkDokument = new PersonhistorikkDokument();
        personhistorikkDokument.statsborgerskapListe = Collections.emptyList();
        when(saksopplysningerService.hentPersonhistorikk(anyLong())).thenReturn(personhistorikkDokument);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR,
            false, Inngangsvilkaar.MANGLER_STATSBORGERSKAP);
    }

    @Test
    public void vurderOgLagreInngangsvilkår_tomDatoErNull_tomDatoSettesTilEttÅrEtterFomDato() throws FunksjonellException, TekniskException {
        ArgumentCaptor<no.nav.melosys.domain.behandlingsgrunnlag.data.Periode> søknadsperiodeCaptor = ArgumentCaptor.forClass(no.nav.melosys.domain.behandlingsgrunnlag.data.Periode.class);

        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), null);
        PersonDokument personDokument = new PersonDokument();
        personDokument.statsborgerskap = Land.av(FINLAND);
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), søknadsperiodeCaptor.capture())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        no.nav.melosys.domain.behandlingsgrunnlag.data.Periode søknadsperiode = søknadsperiodeCaptor.getValue();
        assertThat(søknadsperiode.getTom()).isEqualTo(LocalDate.now().plusYears(2));
    }

    @Test
    public void vurderOgLagreInngangsvilkår_feil_girBegrunnelse() throws TekniskException, FunksjonellException {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        PersonDokument personDokument = new PersonDokument();
        personDokument.statsborgerskap = Land.av(FINLAND);
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        var feilmelding = new Feilmelding();
        feilmelding.setKategori(Kategori.TEKNISK_FEIL);
        feilmelding.setMelding("FEIL!!!");
        res.setFeilmeldinger(Collections.singletonList(feilmelding));
        res.setKvalifisererForEf883_2004(Boolean.FALSE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(eq(personDokument.statsborgerskap), eq(Set.copyOf(tilIso3(landkoder))), eq(periode));
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR,
            false, Inngangsvilkaar.TEKNISK_FEIL);
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
