package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.sak.ArkivsakService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettArkivsakTest {

    @Mock
    private ArkivsakService arkivsakService;
    @Mock
    private FagsakService fagsakService;

    private OpprettArkivsak opprettArkivsak;

    private final OppgaveFactory oppgaveFactory = new OppgaveFactory();

    @BeforeEach
    public void setUp() {
        opprettArkivsak = new OpprettArkivsak(fagsakService, arkivsakService, oppgaveFactory);
    }

    @Test
    void utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet() {
        final long forventetArkivsakID = 1234432;

        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(arkivsakService.opprettSakForBruker(fagsak.getSaksnummer(), oppgaveFactory.utledTema(fagsak.getType(), fagsak.getTema(), behandling.getTema(), behandling.getType()), FagsakTestFactory.BRUKER_AKTØR_ID)).thenReturn(forventetArkivsakID);
        opprettArkivsak.utfør(prosessinstans);

        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(forventetArkivsakID);
    }

    @Test
    void utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet_brukFagsakTema() {
        final long forventetArkivsakID = 1234432;

        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.YRKESAKTIV);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(arkivsakService.opprettSakForBruker(fagsak.getSaksnummer(), oppgaveFactory.utledTema(fagsak.getType(), fagsak.getTema(), behandling.getTema(), behandling.getType()),
            FagsakTestFactory.BRUKER_AKTØR_ID)).thenReturn(forventetArkivsakID);
        opprettArkivsak.utfør(prosessinstans);

        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(forventetArkivsakID);
    }

    @Test
    void utfør_virksomhetErHovedpart_oppretterSakForVirksomhet() {
        final long forventetArkivsakID = 1234432;

        Fagsak fagsak = FagsakTestFactory.builder().medVirksomhet().build();

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.YRKESAKTIV);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(arkivsakService
            .opprettSakForVirksomhet(fagsak.getSaksnummer(), oppgaveFactory.utledTema(fagsak.getType(), fagsak.getTema(), behandling.getTema(), behandling.getType()), FagsakTestFactory.ORGNR))
            .thenReturn(forventetArkivsakID);


        opprettArkivsak.utfør(prosessinstans);


        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(forventetArkivsakID);
    }

    @Test
    void utfør_arkivsakIDEksisterer_kasterException() {
        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettArkivsak.utfør(prosessinstans))
            .withMessageContaining("allerede knyttet til");

    }

    @Test
    void utfør_harVerkenBrukerIDEllerVirksomhetOrgnr_kasterException() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.YRKESAKTIV);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettArkivsak.utfør(prosessinstans))
            .withMessageContaining("Finner verken bruker eller virksomhet tilknyttet fagsak MEL-test");

    }
}
