package no.nav.melosys.saksflyt.steg.aou.mottak;

import java.util.Optional;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.FagsakOgBehandlingFelles;
import no.nav.melosys.service.sak.FagsakService;
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
    private FagsakOgBehandlingFelles fagsakOgBehandlingFelles;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    @Before
    public void setUp() {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(fagsakService, fagsakOgBehandlingFelles);
    }

    @Test
    public void utførSteg_ikkeEndring_VerifiserNyFagsakOgBehandling() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(false);
        Fagsak fagsak = hentFagsak();
        when(fagsakOgBehandlingFelles.opprettFagsakOgBehandling(any(), any(), anyString(), anyString(), anyLong(), any()))
            .thenReturn(fagsak);

        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(fagsakOgBehandlingFelles).opprettFagsakOgBehandling(any(), any(), anyString(), anyString(), anyLong(), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE);
    }

    @Test
    public void utførSteg_erEndring_verifiserNyBehandling() throws Exception {
        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        Prosessinstans prosessinstans = hentProsessinstans(true);
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(fagsakOgBehandlingFelles).opprettBehandlingPåEksisterendeFagsak(any(), any(), any(), anyString(), anyString(), anyLong());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE);
    }

    @Test(expected = TekniskException.class)
    public void utførSteg_ikkeRegistreringUnntakType_kasterException() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(true);
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        opprettFagsakOgBehandling.utfør(prosessinstans);
    }

    private Prosessinstans hentProsessinstans(boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK);
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