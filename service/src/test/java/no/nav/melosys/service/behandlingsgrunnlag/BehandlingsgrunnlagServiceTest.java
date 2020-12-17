package no.nav.melosys.service.behandlingsgrunnlag;

import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.*;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehandlingsgrunnlagServiceTest {

    @Mock
    private BehandlingsgrunnlagRepository behandlingsgrunnlagRepository;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private JoarkFasade joarkFasade;

    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Captor
    private ArgumentCaptor<Behandlingsgrunnlag> behandlingsgrunnlagArgumentCaptor;

    private final long behandlingID = 123332211;
    private final String journalpostID = "123321";

    @BeforeEach
    public void setup() {
        behandlingsgrunnlagService = new BehandlingsgrunnlagService(behandlingsgrunnlagRepository, behandlingService, joarkFasade);
    }

    @Test
    void hentBehandlingsgrunnlagForBehandlingID_finnes_returnerBehandlingsgrunnlag() throws IkkeFunnetException {
        when(behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID))
            .thenReturn(Optional.of(new Behandlingsgrunnlag()));
        assertThat(behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID)).isNotNull();
    }

    @Test
    void hentBehandlingsgrunnlagForBehandlingID_finnesIkke_kastException() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingsgrunnlagService.hentBehandlingsgrunnlag(1))
            .withMessageContaining("Finner ikke behandlingsgrunnlag for behandling 1");
    }

    @Test
    void opprettSøknadGrunnlag_finnesIkkeFraFør_blirOpprettet() throws FunksjonellException, IntegrasjonException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(eq(behandling.getInitierendeJournalpostId()))).thenReturn(LocalDate.now());
        Soeknad soeknad = new Soeknad();
        behandlingsgrunnlagService.opprettSøknadYrkesaktiveEøs(behandlingID, soeknad);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(Soeknad.class);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void oppdaterBehandlingsgrunnlag_eksisterer_oppdatererBehandlingsgrunnlagData() throws IkkeFunnetException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        BehandlingsgrunnlagData originalData = new BehandlingsgrunnlagData();
        String originalJsonData = objectMapper.writeValueAsString(originalData);
        behandlingsgrunnlag.setJsonData(originalJsonData);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        when(behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)).thenReturn(Optional.of(behandlingsgrunnlag));

        BehandlingsgrunnlagData nyData = new Soeknad();
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(nyData));

        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, jsonNode);
        verify(behandlingsgrunnlagRepository).saveAndFlush(any(Behandlingsgrunnlag.class));

        assertThat(behandlingsgrunnlag.getJsonData()).isNotEqualTo(originalJsonData);
    }

    @Test
    void opprettSedGrunnlag_harRettType() throws FunksjonellException, IntegrasjonException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(eq(behandling.getInitierendeJournalpostId()))).thenReturn(LocalDate.now());
        SedGrunnlag sedGrunnlag = new SedGrunnlag();
        behandlingsgrunnlagService.opprettSedGrunnlag(behandlingID, sedGrunnlag);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(SedGrunnlag.class);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SED);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void opprettSøknadFolketrygden_harRettType() throws FunksjonellException, IntegrasjonException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(eq(behandling.getInitierendeJournalpostId()))).thenReturn(LocalDate.now());
        SoeknadFtrl sedGrunnlag = new SoeknadFtrl();
        behandlingsgrunnlagService.opprettSøknadFolketrygden(behandlingID, sedGrunnlag);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(SoeknadFtrl.class);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setInitierendeJournalpostId(journalpostID);
        return behandling;
    }
}