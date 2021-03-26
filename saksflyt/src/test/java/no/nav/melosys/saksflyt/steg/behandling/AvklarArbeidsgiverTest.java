package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvklarArbeidsgiverTest {
    @Mock
    AktoerService aktoerService;
    @Mock
    BehandlingService behandlingService;
    @Mock
    BehandlingsresultatService behandlingsresultatService;

    Behandling behandling = new Behandling();
    @Mock
    AvklarteVirksomheterSystemService avklarteVirksomheterService;

    private AvklarArbeidsgiver avklarArbeidsgiver;
    private Prosessinstans prosessinstans;
    private AvklartVirksomhet avklartVirksomhet;
    private Fagsak fagsak;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        aktoerService = mock(AktoerService.class);
        avklarArbeidsgiver = new AvklarArbeidsgiver(aktoerService, avklarteVirksomheterService, behandlingService, behandlingsresultatService);

        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);

        fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");
        behandling.setFagsak(fagsak);
        behandling.setId(1L);
        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        avklartVirksomhet =
            new AvklartVirksomhet("Test", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
    }

    @Test
    void utfør_medAvklartNorskVirksomhet_arbeidsgiveraktørOpprettes() throws FunksjonellException, TekniskException {
        AktoerRepository aktoerRepository = mock(AktoerRepository.class);
        AvklarArbeidsgiver steg = new AvklarArbeidsgiver(new AktoerService(aktoerRepository), avklarteVirksomheterService,
            behandlingService, behandlingsresultatService);

        List<AvklartVirksomhet> avklarteVirksomheter = Collections.singletonList(avklartVirksomhet);
        when(avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any())).thenReturn(avklarteVirksomheter);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        steg.utfør(prosessinstans);

        verify(aktoerRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));

        Aktoer aktoer = new Aktoer();
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktoer.setOrgnr("123456789");
        verify(aktoerRepository).save(eq(aktoer));
    }

    @Test
    void utfør_utenAvklartNorskVirksomhet_arbeidsgiveraktorerSlettes() throws FunksjonellException, TekniskException {
        AktoerRepository aktoerRepository = mock(AktoerRepository.class);
        AvklarArbeidsgiver steg = new AvklarArbeidsgiver(new AktoerService(aktoerRepository), avklarteVirksomheterService,
            behandlingService, behandlingsresultatService);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        steg.utfør(prosessinstans);
        verify(aktoerRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));
        verify(aktoerRepository, never()).save(any());
    }

    @Test
    void utfør_iverksettVedtakArt12_arbeidsgiverAktoererSkalOpprettes() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        avklarArbeidsgiver.utfør(prosessinstans);
        verify(aktoerService).erstattEksisterendeArbeidsgiveraktører(any(), any());
    }

    @Test
    void utfør_iverksettVedtakArt13_arbeidsgiverAktoererSkalIkkeOpprettes() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        avklarArbeidsgiver.utfør(prosessinstans);
        verify(aktoerService, never()).erstattEksisterendeArbeidsgiveraktører(any(), any());
    }

    @Test
    void utfør_iverksettVedtakAvslagManglendeOppl_arbeidsgiverAktoererSkalIkkeOpprettes() throws FunksjonellException, TekniskException {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        behandlingsresultat.setLovvalgsperioder(new HashSet<>());
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        avklarArbeidsgiver.utfør(prosessinstans);
        verify(aktoerService).erstattEksisterendeArbeidsgiveraktører(any(), any());
    }
}