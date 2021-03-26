package no.nav.melosys.saksflyt.steg.behandling;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingFraSedTest {

    @Mock
    private FagsakService fagsakService;

    private OpprettFagsakOgBehandlingFraSed opprettFagsakOgBehandlingFraSed;

    @Captor
    private ArgumentCaptor<OpprettSakRequest> opprettSakRequestArgumentCaptor;

    @Before
    public void setUp() {
        opprettFagsakOgBehandlingFraSed = new OpprettFagsakOgBehandlingFraSed(fagsakService);
        when(fagsakService.nyFagsakOgBehandling(any())).thenReturn(hentFagsak());
    }

    @Test
    public void utfør_prosessTypeAnmodningsUnntak_verifiserNyFagsakOgBehandlingBlirOpprettet() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        opprettFagsakOgBehandlingFraSed.utfør(prosessinstans);
        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstype()).isEqualTo(Sakstyper.EU_EOS);
    }

    @Test
    public void utfør__verifiserNyFagsakOgBehandlingBlirOpprettet() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        opprettFagsakOgBehandlingFraSed.utfør(prosessinstans);
        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstype()).isEqualTo(Sakstyper.UKJENT);
    }

    private Prosessinstans hentProsessinstans(Behandlingstema behandlingstema) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, "321");
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);

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