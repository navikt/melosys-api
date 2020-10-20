package no.nav.melosys.saksflyt.steg.behandling;

import java.time.Instant;
import java.util.ArrayList;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.OPPRETTET;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReplikerBehandlingTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private ReplikerBehandling replikerBehandling;

    private final Fagsak fagsak = new Fagsak();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        replikerBehandling = new ReplikerBehandling(fagsakService, behandlingService);
        fagsak.setStatus(Saksstatuser.LOVVALG_AVKLART);
        fagsak.setBehandlinger(new ArrayList<>());
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "MelTest-1");
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        doReturn(fagsak).when(fagsakService).hentFagsak("MelTest-1");
    }

    @Test
    void utfør_manglendeInaktivBehandling_feiler() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("ingen avsluttet behandling");
    }

    @Test
    void utfør_eksisterendeInaktivBehandling_settStegOpprettOppgave() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setRegistrertDato(Instant.now());
        fagsak.getBehandlinger().add(behandling);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setFagsak(fagsak);
        replikertBehandling.setId(11L);
        doReturn(replikertBehandling).when(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling,  Behandlingsstatus.OPPRETTET, Behandlingstyper.ENDRET_PERIODE);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(fagsak.getStatus()).isEqualTo(OPPRETTET);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
    }
}