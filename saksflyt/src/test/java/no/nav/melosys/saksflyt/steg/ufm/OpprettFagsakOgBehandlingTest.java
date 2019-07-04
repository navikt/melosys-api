package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Optional;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    @Before
    public void setUp() throws Exception {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(fagsakService,behandlingService);
        when(fagsakService.nyFagsakOgBehandling(any())).thenReturn(hentFagsak());
    }

    @Test
    public void utførSteg_ikkeEndring_VerifiserNyFagsakOgBehandling() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(false);
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(fagsakService).nyFagsakOgBehandling(any(OpprettSakRequest.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET);
    }

    @Test
    public void utførSteg_erEndring_verifiserNyBehandling() throws Exception {
        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        Prosessinstans prosessinstans = hentProsessinstans(true);
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(behandlingService).nyBehandling(any(Fagsak.class), eq(Behandlingsstatus.UNDER_BEHANDLING), eq(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD), eq("123"), eq("321"));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET);
    }

    @Test(expected = TekniskException.class)
    public void utførSteg_ikkeRegistreringUnntakType_kasterException() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(true);
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        opprettFagsakOgBehandling.utfør(prosessinstans);
    }

    private Prosessinstans hentProsessinstans(boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, "321");
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, erEndring);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123);
        return prosessinstans;
    }

    private Fagsak hentFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);

        fagsak.setBehandlinger(Lists.newArrayList(behandling));
        return fagsak;
    }
}