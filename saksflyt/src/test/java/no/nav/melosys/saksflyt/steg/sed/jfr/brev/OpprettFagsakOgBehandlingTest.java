package no.nav.melosys.saksflyt.steg.sed.jfr.brev;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private TpsFasade tpsFasade;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    private Prosessinstans prosessinstans = new Prosessinstans();

    @Before
    public void setup() throws FunksjonellException {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(fagsakService, tpsFasade);

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, "321");
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "1234");

        Behandling aktivBehandling = new Behandling();
        aktivBehandling.setStatus(Behandlingsstatus.OPPRETTET);
        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(Collections.singletonList(aktivBehandling));
        fagsak.setSaksnummer("MEL-111");
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);
    }

    @Test
    public void utfør_harAktørId_forventIngenKallMotTps() throws MelosysException {
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "111");

        opprettFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).nyFagsakOgBehandling(any(OpprettSakRequest.class));
        verify(tpsFasade, never()).hentAktørIdForIdent(anyString());
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSNUMMER)).isEqualTo("MEL-111");
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT);
    }

    @Test
    public void utfør_ingenAktørId_forventKallMotTps() throws MelosysException {
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn("111");
        opprettFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).nyFagsakOgBehandling(any(OpprettSakRequest.class));
        verify(tpsFasade).hentAktørIdForIdent(anyString());
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSNUMMER)).isEqualTo("MEL-111");
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isEqualTo("111");
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT);
    }
}