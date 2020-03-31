package no.nav.melosys.saksflyt.steg.hs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HenleggSakTest {
    @Mock
    private FagsakService fagsakService;

    private HenleggSak henleggSak;

    @Before
    public void setup() {
        henleggSak = new HenleggSak(fagsakService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        final String saksnummer = "MEL-222";
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        behandling.setFagsak(fagsak);

        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);

        henleggSak.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.HENLAGT));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.HS_SEND_BREV);
    }
}