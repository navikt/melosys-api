package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettArkivsakTest {

    @Mock
    private SakService sakService;
    @Mock
    private FagsakService fagsakService;

    private OpprettArkivsak opprettArkivsak;

    private final String aktørID = "1111";
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final Fagsak fagsak = new Fagsak();
    private final Long arkivsakID = 1231231L;

    @Before
    public void setup() throws FunksjonellException, TekniskException {
        opprettArkivsak = new OpprettArkivsak(sakService, fagsakService);

        fagsak.setSaksnummer("MEL-0");

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(fagsak);

        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(aktørID);
        fagsak.getAktører().add(aktoer);

        prosessinstans.setBehandling(behandling);

        when(sakService.opprettSak(any(), any(), any())).thenReturn(arkivsakID);
    }

    @Test
    public void utfør_arkivsakIDBlirOpprettetOgSatt() throws MelosysException {
        opprettArkivsak.utfør(prosessinstans);
        verify(sakService).opprettSak(eq(fagsak.getSaksnummer()), any(Behandlingstema.class), eq(aktørID));
        assertThat(fagsak.getGsakSaksnummer()).isEqualTo(arkivsakID);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.MSA_OPPRETT_OG_FERDIGSTILL_JOURNALPOST);
    }

}