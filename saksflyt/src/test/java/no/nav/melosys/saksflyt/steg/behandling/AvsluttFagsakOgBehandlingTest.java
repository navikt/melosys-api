package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Collections;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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

    private final String saksnummer = "MEL-123";

    @BeforeEach
    public void setUp() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(fagsakService, behandlingService, behandlingsresultatService, saksbehandlingRegler);

        prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK_EOS);

        behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setId(123L);
        behandling.setTema(Behandlingstema.ARBEID_I_UTLANDET);

        fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

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
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
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
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any())).thenReturn(true);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
    }

    @Test
    void utfør_saksstatusIProsessData_behandlingsstatusSatt() {
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        prosessinstans.setData(ProsessDataKey.SAKSSTATUS, Saksstatuser.AVSLUTTET);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.AVSLUTTET);
    }
}
