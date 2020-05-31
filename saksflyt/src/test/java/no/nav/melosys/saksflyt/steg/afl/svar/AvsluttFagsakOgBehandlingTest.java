package no.nav.melosys.saksflyt.steg.afl.svar;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sob.SobService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    @Mock
    private SobService sobService;
    @Mock
    private FagsakService fagsakService;

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    @Before
    public void setup() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(fagsakService, sobService);
    }

    @Test
    public void utfør() throws MelosysException {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("!312312");

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setAktører(Set.of(aktoer));

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);
        verify(sobService).sakOgBehandlingAvsluttet(eq("MEL-123"), eq(1L), eq("!312312"));
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.AVSLUTTET));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}