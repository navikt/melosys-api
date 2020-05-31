package no.nav.melosys.saksflyt.steg.aou.ut.svar;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vedtak.VedtakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FattVedtakEllerOppdaterBehandlingTest {
    private FattVedtakEllerOppdaterBehandling fattVedtakEllerOppdaterBehandling;

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private VedtakService vedtakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    @Captor
    private ArgumentCaptor<Collection<Lovvalgsperiode>> captor;

    private Anmodningsperiode anmodningsperiode = new Anmodningsperiode();

    @Before
    public void setUp() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiode(anmodningsperiode);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        fattVedtakEllerOppdaterBehandling = new FattVedtakEllerOppdaterBehandling(anmodningsperiodeService, behandlingService, behandlingsresultatService, vedtakService, lovvalgsperiodeService);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(anmodningsperiode));
    }

    @Test
    public void utfør_anmodningsperiodeIkkeInnvilget_statusSvarAouMottattBehandling() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setYtterligereInformasjon("hei");
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.AVSLAG);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        fattVedtakEllerOppdaterBehandling.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.AVSLAATT);
    }

    @Test
    public void utfør_anmodningsperiodeInnvilget_fattVedtak() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        fattVedtakEllerOppdaterBehandling.utfør(prosessinstans);

        verify(vedtakService).fattVedtak(eq(behandling.getId()), eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND));
        verify(behandlingsresultatService).oppdaterBehandlingsMaate(anyLong(), any());
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
    }

    @Test
    public void utfør_anmodningsperiodeInnvilgetMedYtteligereInfo_statusSvarAouMottattBehandling() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setYtterligereInformasjon("hei");
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        fattVedtakEllerOppdaterBehandling.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
    }

    @Test
    public void utfør_valideringsfeilFattVedtak_statusSvarAouMottattBehandling() throws MelosysException {
        doThrow(new ValideringException(
            "Kunne ikke fatte vedtak",
            Set.of(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode()))
        ).when(vedtakService).fattVedtak(anyLong(), any(Behandlingsresultattyper.class));

        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        fattVedtakEllerOppdaterBehandling.utfør(prosessinstans);

        verify(vedtakService).fattVedtak(eq(123L), eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND));
        verify(behandlingsresultatService, never()).oppdaterBehandlingsMaate(eq(123L), eq(Behandlingsmaate.DELVIS_AUTOMATISERT));
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
    }
}