package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Optional;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
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
    @Mock
    private EessiService eessiService;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    @Before
    public void setUp() throws Exception {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(fagsakService, behandlingService, eessiService);
        when(fagsakService.nyFagsakOgBehandling(any())).thenReturn(hentFagsak());
    }

    @Test
    public void utførSteg_relasjonFinnesIkke_VerifiserNyFagsakOgBehandling() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans();
        when(eessiService.hentSakForRinasaksnummer(anyString())).thenReturn(Optional.empty());
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(fagsakService).nyFagsakOgBehandling(any(OpprettSakRequest.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPRETT_GSAK_SAK);
    }

    @Test
    public void utførSteg_relasjonFinnes_verifiserNyBehandling() throws Exception {
        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        when(eessiService.hentSakForRinasaksnummer(anyString())).thenReturn(Optional.of(123L));
        Prosessinstans prosessinstans = hentProsessinstans();
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(behandlingService).nyBehandling(any(Fagsak.class), eq(Behandlingsstatus.UNDER_BEHANDLING), eq(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD), eq("123"), eq("321"));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET);
    }

    @Test(expected = TekniskException.class)
    public void utførSteg_ikkeRegistreringUnntakType_kasterException() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        opprettFagsakOgBehandling.utfør(prosessinstans);
    }

    @Test(expected = TekniskException.class)
    public void utførSteg_relasjonFinnesMenFinnerIkkeFagsak_forventTekniskException() throws Exception {
        when(eessiService.hentSakForRinasaksnummer(anyString())).thenReturn(Optional.of(1L));
        when(fagsakService.hentFagsakFraGsakSaksnummer(any())).thenReturn(Optional.empty());
        opprettFagsakOgBehandling.utfør(hentProsessinstans());
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, "321");
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer("123rina");

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

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