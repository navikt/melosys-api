package no.nav.melosys.service.behandlingsgrunnlag;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.service.BehandlingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingsgrunnlagServiceTest {

    @Mock
    private BehandlingsgrunnlagRepository behandlingsgrunnlagRepository;
    @Mock
    private BehandlingService behandlingService;

    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private final long behandlingID = 123332211;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Behandlingsgrunnlag> behandlingsgrunnlagArgumentCaptor;

    @Before
    public void setup() {
        behandlingsgrunnlagService = new BehandlingsgrunnlagService(behandlingsgrunnlagRepository, behandlingService);
    }

    @Test
    public void hentBehandlingsgrunnlagForBehandlingID_finnes_returnerBehandlingsgrunnlag() throws IkkeFunnetException {
        when(behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID))
            .thenReturn(Optional.of(new Behandlingsgrunnlag()));
        assertThat(behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID)).isNotNull();
    }

    @Test
    public void hentBehandlingsgrunnlagForBehandlingID_finnesIkke_kastException() throws IkkeFunnetException {
        expectedException.expect(IkkeFunnetException.class);
        expectedException.expectMessage("Finner ikke behandlingsgrunnlag for behandling 1");
        behandlingsgrunnlagService.hentBehandlingsgrunnlag(1);
    }

    @Test
    public void opprettSøknadGrunnlag_finnesIkkeFraFør_blirOpprettet() throws FunksjonellException {
        final long behandlingID = 1234L;
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        behandlingsgrunnlagService.opprettSøknadGrunnlag(behandlingID, soeknadDokument);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(SoeknadDokument.class);
        assertThat(opprettet.getType()).isEqualTo(BehandlingsGrunnlagType.SØKNAD);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void oppdaterBehandlingsgrunnlag_eksisterer_oppdatererBehandlingsgrunnlagData() throws IkkeFunnetException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        BehandlingsgrunnlagData originalData = new BehandlingsgrunnlagData();
        originalData.arbeidNorge.kontaktNavn = "Nils Nilsersenenen";
        String originalJsonData = objectMapper.writeValueAsString(originalData);
        behandlingsgrunnlag.setJsonData(originalJsonData);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        when(behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)).thenReturn(Optional.of(behandlingsgrunnlag));

        BehandlingsgrunnlagData nyData = new SoeknadDokument();
        nyData.arbeidNorge.kontaktNavn = "Per Pererersen";
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(nyData));

        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, jsonNode);
        verify(behandlingsgrunnlagRepository).saveAndFlush(any(Behandlingsgrunnlag.class));

        assertThat(behandlingsgrunnlag.getJsonData()).isNotEqualTo(originalJsonData);
    }

    @Test
    public void opprettSedGrunnlag_harRettType() throws FunksjonellException {
        final long behandlingID = 1234L;
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        SedGrunnlag sedGrunnlag = new SedGrunnlag();
        behandlingsgrunnlagService.opprettSedGrunnlag(behandlingID, sedGrunnlag);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(SedGrunnlag.class);
        assertThat(opprettet.getType()).isEqualTo(BehandlingsGrunnlagType.SED);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
    }
}