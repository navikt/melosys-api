package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.saksflytapi.domain.*;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvsluttFagsakOgBehandlingTest {

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;

    private Behandling behandling;
    private Fagsak fagsak;
    private Lovvalgsperiode lovvalgsperiode;
    private Prosessinstans prosessinstans;


    @BeforeEach
    public void setUp() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(fagsakService, behandlingService, behandlingsresultatService, saksbehandlingRegler);

        fagsak = FagsakTestFactory.builder().build();
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medType(Behandlingstyper.FØRSTEGANG)
            .medId(123L)
            .medTema(Behandlingstema.YRKESAKTIV)
            .medFagsak(fagsak)
            .build();
        fagsak.getBehandlinger().add(behandling);
        prosessinstans = ProsessinstansTestFactory.builderWithDefaults()
            .medType(ProsessType.IVERKSETT_VEDTAK_EOS)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .build();

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        behandlingsresultat.setBehandling(behandling);

        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId()))
            .thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_erArtikkel12_behandlingOgFagsakAvsluttet() {
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
    }

    @Test
    void utfør_erArtikkel13_behandlingsstatusMidlertidigLovvalgsbeslutning() {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(behandlingService).endreStatus(behandling.getId(), Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
    }

    @Test
    void utfør_erArtikkel13OgBehandlingstemaA1AnmodningOmUnntakPapir_behandlingOgFagsakAvsluttet() {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        behandling.setTema(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR);
        fagsak.setTema(Sakstemaer.UNNTAK);
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any())).thenReturn(true);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
    }

    @Test
    void utfør_saksstatusIProsessData_behandlingsstatusSatt() {
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.SAKSSTATUS, Saksstatuser.AVSLUTTET)
            .build();

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.AVSLUTTET);
    }

    @Test
    void utfør_fattIverksettVedtakÅrsavregningProsess_MedFlereEnnEnBehandlingAvslutterKunBehandling() {
        prosessinstans = prosessinstans.toBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING)
            .build();

        behandling.setType(Behandlingstyper.ÅRSAVREGNING);

        var behandling2 = BehandlingTestFactory.builderWithDefaults()
            .medId(1234L)
            .medType(Behandlingstyper.ÅRSAVREGNING)
            .build();
        fagsak.getBehandlinger().add(behandling2);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);


        avsluttFagsakOgBehandling.utfør(prosessinstans);


        verify(behandlingService).avsluttBehandling(behandling.getId());
    }

    @Test
    void utfør_fattIverksettVedtakÅrsavregningProsess_MedKunEnBehandlingAvslutterKunSakOgBehandling() {
        prosessinstans = prosessinstans.toBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING)
            .build();
        behandling.setType(Behandlingstyper.ÅRSAVREGNING);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);


        avsluttFagsakOgBehandling.utfør(prosessinstans);


        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET);
    }
}
