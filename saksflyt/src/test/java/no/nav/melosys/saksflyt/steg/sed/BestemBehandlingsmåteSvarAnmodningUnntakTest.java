package no.nav.melosys.saksflyt.steg.sed;

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
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vedtak.VedtakServiceFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BestemBehandlingsmåteSvarAnmodningUnntakTest {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private VedtakServiceFasade vedtakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private BestemBehandlingsmåteSvarAnmodningUnntak bestemBehandlingsmåteSvarAnmodningUnntak;

    @Captor
    private ArgumentCaptor<Collection<Lovvalgsperiode>> captor;

    private final Anmodningsperiode anmodningsperiode = new Anmodningsperiode();

    @BeforeEach
    public void setUp() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiode(anmodningsperiode);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        bestemBehandlingsmåteSvarAnmodningUnntak = new BestemBehandlingsmåteSvarAnmodningUnntak(anmodningsperiodeService, behandlingService, behandlingsresultatService, vedtakService, lovvalgsperiodeService);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(anmodningsperiode));
    }

    @Test
    void utfør_anmodningsperiodeIkkeInnvilget_statusSvarAouMottattBehandling() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setYtterligereInformasjon("hei");
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.AVSLAG);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(lagBehandling());

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.AVSLAATT);
    }

    @Test
    void utfør_anmodningsperiodeInnvilgetOgStatusAouSendt_fattVedtak() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling behandling = lagBehandling(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(behandling.getId())).thenReturn(behandling);

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans);

        verify(vedtakService).fattVedtak(eq(behandling.getId()), eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND));
        verify(behandlingsresultatService).oppdaterBehandlingsMaate(anyLong(), any());
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
    }

    @Test
    void utfør_anmodningsperiodeInnvilgetStatusIkkeAouSendt_statusSvarAouMottattBehandling() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling behandling = lagBehandling(Behandlingsstatus.VURDER_DOKUMENT);
        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(behandling.getId())).thenReturn(behandling);

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans);

        verify(vedtakService, never()).fattVedtak(anyLong(), any());
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
    }

    @Test
    void utfør_anmodningsperiodeInnvilgetMedYtteligereInfo_statusSvarAouMottattBehandling() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setYtterligereInformasjon("hei");
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(lagBehandling());

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
    }

    @Test
    void utfør_valideringsfeilFattVedtak_statusSvarAouMottattBehandling() throws MelosysException {
        KontrollfeilDto kontrollfeilDto = new KontrollfeilDto();
        kontrollfeilDto.setKode(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode());

        doThrow(new ValideringException(
            "Kunne ikke fatte vedtak",
            Set.of(kontrollfeilDto))
        ).when(vedtakService).fattVedtak(anyLong(), any(Behandlingsresultattyper.class));

        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSvarAnmodningUnntak(new SvarAnmodningUnntak());
        melosysEessiMelding.getSvarAnmodningUnntak().setBeslutning(SvarAnmodningUnntak.Beslutning.INNVILGELSE);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling behandling = lagBehandling();
        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(behandling.getId())).thenReturn(behandling);

        bestemBehandlingsmåteSvarAnmodningUnntak.utfør(prosessinstans);

        verify(vedtakService).fattVedtak(eq(123L), eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND));
        verify(behandlingsresultatService, never()).oppdaterBehandlingsMaate(eq(123L), eq(Behandlingsmaate.DELVIS_AUTOMATISERT));
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.SVAR_ANMODNING_MOTTATT));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), captor.capture());

        Collection<Lovvalgsperiode> lagredeLovvalgsperioder = captor.getValue();
        assertThat(lagredeLovvalgsperioder).hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagredeLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        return behandling;
    }

    private static Behandling lagBehandling(Behandlingsstatus behandlingsstatus) {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setStatus(behandlingsstatus);
        return behandling;
    }
}