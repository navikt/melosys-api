package no.nav.melosys.saksflyt.steg.behandling;

import com.google.common.collect.Lists;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettFagsakOgBehandlingFraSedTest {

    @Mock
    private FagsakService fagsakService;

    private final FakeUnleash unleash = new FakeUnleash();
    private OpprettFagsakOgBehandlingFraSed opprettFagsakOgBehandlingFraSed;

    @Captor
    private ArgumentCaptor<OpprettSakRequest> opprettSakRequestArgumentCaptor;

    @BeforeEach
    void setUp() {
        opprettFagsakOgBehandlingFraSed = new OpprettFagsakOgBehandlingFraSed(unleash, fagsakService);
        when(fagsakService.nyFagsakOgBehandling(any())).thenReturn(lagFagsak());
        unleash.enableAll();
    }

    @Test
    void utfør_verifiserNyFagsakOgBehandlingBlirOpprettet() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);


        opprettFagsakOgBehandlingFraSed.utfør(prosessinstans);


        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstype()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstema()).isEqualTo(Sakstemaer.UNNTAK);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getBehandlingstype()).isEqualTo(Behandlingstyper.FØRSTEGANG);
    }

    @Test
    void utfør_toggleDisabled_verifiserNyFagsakOgBehandlingBlirOpprettet() {
        unleash.disableAll();
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);


        opprettFagsakOgBehandlingFraSed.utfør(prosessinstans);


        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstype()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstema()).isEqualTo(Sakstemaer.UNNTAK);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getBehandlingstype()).isEqualTo(Behandlingstyper.SED);
    }

    private Prosessinstans lagProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, "321");
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123);
        prosessinstans.setData(ProsessDataKey.SAKSTEMA, Sakstemaer.UNNTAK);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.BESLUTNING_LOVVALG_NORGE);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer("123rina");

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        return prosessinstans;
    }

    private Fagsak lagFagsak() {
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
