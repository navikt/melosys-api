package no.nav.melosys.saksflyt.steg.jfr;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.sak.ArkivsakService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.TemaFactory.fraBehandlingstema;
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

    private final OppgaveFactory oppgaveFactory = new OppgaveFactory(new FakeUnleash());
    @BeforeEach
    public void setUp() {
        opprettArkivsak = new OpprettArkivsak(fagsakService, arkivsakService, oppgaveFactory);
    }

    @Test
    void utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet() {
        final long forventetArkivsakID = 1234432;

        String aktørID = "4214323324";
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-4321");
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

        Aktoer bruker = new Aktoer();
        bruker.setAktørId(aktørID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.getAktører().add(bruker);

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(arkivsakService.opprettSakForBruker(fagsak.getSaksnummer(), fraBehandlingstema(behandling.getTema()),
            aktørID)).thenReturn(forventetArkivsakID);
        opprettArkivsak.utfør(prosessinstans);

        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(forventetArkivsakID);
    }

    @Test
    void utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet_brukFagsakTema() {
        final long forventetArkivsakID = 1234432;

        String aktørID = "4214323324";
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-4321");
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

        Aktoer bruker = new Aktoer();
        bruker.setAktørId(aktørID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.getAktører().add(bruker);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(arkivsakService.opprettSakForBruker(fagsak.getSaksnummer(), oppgaveFactory.utledTema(fagsak.getTema()),
            aktørID)).thenReturn(forventetArkivsakID);
        opprettArkivsak.utfør(prosessinstans);

        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(forventetArkivsakID);
    }

    @Test
    void utfør_virksomhetErHovedpart_oppretterSakForVirksomhet() {
        final long forventetArkivsakID = 1234432;

        String orgnr = "999999999";
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-4321");
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

        Aktoer virksomhet = new Aktoer();
        virksomhet.setOrgnr(orgnr);
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.getAktører().add(virksomhet);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(arkivsakService
            .opprettSakForVirksomhet(fagsak.getSaksnummer(), oppgaveFactory.utledTema(fagsak.getTema()), orgnr))
            .thenReturn(forventetArkivsakID);


        opprettArkivsak.utfør(prosessinstans);


        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(forventetArkivsakID);
    }

    @Test
    void utfør_arkivsakIDEksisterer_kasterException() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-4321");
        fagsak.setGsakSaksnummer(11111L);

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
        Fagsak fagsak = new Fagsak();
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.setSaksnummer("MEL-4321");

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettArkivsak.utfør(prosessinstans))
            .withMessageContaining("Finner verken bruker eller virksomhet tilknyttet fagsak MEL-4321");

    }
}
