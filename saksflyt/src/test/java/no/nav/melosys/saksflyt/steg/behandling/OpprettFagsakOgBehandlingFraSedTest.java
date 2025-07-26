package no.nav.melosys.saksflyt.steg.behandling;

import java.time.LocalDate;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettFagsakOgBehandlingFraSedTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private JoarkFasade joarkFasade;

    private OpprettFagsakOgBehandlingFraSed opprettFagsakOgBehandlingFraSed;

    @Captor
    private ArgumentCaptor<OpprettSakRequest> opprettSakRequestArgumentCaptor;

    @BeforeEach
    void setUp() {
        opprettFagsakOgBehandlingFraSed = new OpprettFagsakOgBehandlingFraSed(fagsakService, joarkFasade);
        when(fagsakService.nyFagsakOgBehandling(any())).thenReturn(lagFagsak());
    }

    @Test
    void utfør_verifiserNyFagsakOgBehandlingBlirOpprettet() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        when(joarkFasade.hentMottaksDatoForJournalpost(anyString())).thenReturn(LocalDate.EPOCH);

        opprettFagsakOgBehandlingFraSed.utfør(prosessinstans);


        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstype()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getSakstema()).isEqualTo(Sakstemaer.UNNTAK);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getBehandlingstype()).isEqualTo(Behandlingstyper.FØRSTEGANG);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getBehandlingsårsaktype()).isEqualTo(Behandlingsaarsaktyper.SED);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getMottaksdato()).isNotNull();
    }

    private Prosessinstans lagProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SAKSTEMA, Sakstemaer.UNNTAK);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.BESLUTNING_LOVVALG_NORGE);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer("123rina");
        melosysEessiMelding.setJournalpostId("123");

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        return prosessinstans;
    }

    private Fagsak lagFagsak() {
        Behandling behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .build();

        return FagsakTestFactory.lagFagsakMedBehandlinger(behandling);
    }}
