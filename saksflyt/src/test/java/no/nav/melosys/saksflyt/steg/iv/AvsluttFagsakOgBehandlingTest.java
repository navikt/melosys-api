package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
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

    private Behandling behandling;
    private Fagsak fagsak;
    private Lovvalgsperiode lovvalgsperiode;
    private Prosessinstans prosessinstans;

    private final String saksnummer = "MEL-123";

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(fagsakService, behandlingService, behandlingsresultatService);

        prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);

        behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setId(123L);

        fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));

        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId())))
            .thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_erArtikkel12_behandlingOgFagsakAvsluttet() throws FunksjonellException, TekniskException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        avsluttFagsakOgBehandling.utfør(prosessinstans);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.LOVVALG_AVKLART));
    }

    @Test
    void utfør_erArtikkel13_behandlingsstatusMidlertidigLovvalgsbeslutning() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        avsluttFagsakOgBehandling.utfør(prosessinstans);
        verify(behandlingService).oppdaterStatus(eq(behandling.getId()), eq(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING));
    }
}