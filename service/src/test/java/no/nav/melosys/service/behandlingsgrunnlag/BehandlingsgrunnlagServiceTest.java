package no.nav.melosys.service.behandlingsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.*;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
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
import static org.mockito.Mockito.*;

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

    FakeUnleash fakeUnleash = new FakeUnleash();

    @BeforeEach
    public void setup() {
        behandlingsgrunnlagService = new BehandlingsgrunnlagService(behandlingsgrunnlagRepository, behandlingService, joarkFasade, fakeUnleash);
    }

    @Test
    void hentBehandlingsgrunnlagForBehandlingID_finnes_returnerBehandlingsgrunnlag() {
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
    void opprettEøsSøknadGrunnlag_finnesIkkeFraFør_blirOpprettet() {
        Behandling behandling = lagBehandling(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());
        Periode periode = new Periode();
        Soeknadsland soeknadsland = new Soeknadsland();


        behandlingsgrunnlagService.opprettSøknad(behandling, periode, soeknadsland);


        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(Soeknad.class);
        assertThat(opprettet.getBehandlingsgrunnlagdata().periode).isEqualTo(periode);
        assertThat(opprettet.getBehandlingsgrunnlagdata().soeknadsland).isEqualTo(soeknadsland);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void oppdaterBehandlingsgrunnlagJson_behandlingsgrunnlagEksisterer_oppdatererBehandlingsgrunnlagData() throws JsonProcessingException {
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
    void oppdaterBehandlingsgrunnlag_behandlingsgrunnlagJsonDataIkkeSatt_setterJsonDataOgLagrerBehandlingsgrunnlag() throws JsonProcessingException {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.periode = new Periode(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2010, 1, 1)
        );
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);

        verify(behandlingsgrunnlagRepository).saveAndFlush(behandlingsgrunnlagArgumentCaptor.capture());
        JsonNode jsonNode = new ObjectMapper().readTree(behandlingsgrunnlagArgumentCaptor.getValue().getJsonData());
        String periode = jsonNode.get("periode").toString();
        assertThat(periode)
            .isEqualTo("{" +
                "\"fom\":[2000,1,1]," +
                "\"tom\":[2010,1,1]" +
                "}");
    }

    @Test
    void oppdaterBehandlingsgrunnlagPeriodeOgLand_eksisterer_oppdatererPeriodeOgLand() {
        ArgumentCaptor<Behandlingsgrunnlag> captor = ArgumentCaptor.forClass(Behandlingsgrunnlag.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());

        when(behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)).thenReturn(Optional.of(behandlingsgrunnlag));

        var periode = new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31));
        var soeknadsland = new Soeknadsland(List.of("UK"), false);

        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlagPeriodeOgLand(behandlingID, periode, soeknadsland);

        verify(behandlingsgrunnlagRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getBehandlingsgrunnlagdata().periode).isEqualTo(periode);
        assertThat(captor.getValue().getBehandlingsgrunnlagdata().soeknadsland).isEqualTo(soeknadsland);
    }

    @Test
    void opprettSedGrunnlag_harRettType() {
        Behandling behandling = lagBehandling(Sakstyper.EU_EOS, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());
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
    void opprettSøknadFolketrygden_harRettType() {
        Behandling behandling = lagBehandling(Sakstyper.FTRL, Behandlingstema.YRKESAKTIV);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());


        behandlingsgrunnlagService.opprettSøknad(behandling, null, null);


        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(SoeknadFtrl.class);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void opprettSøknadTrygdeavtale_harRettType() {
        Behandling behandling = lagBehandling(Sakstyper.TRYGDEAVTALE, Behandlingstema.YRKESAKTIV);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());


        behandlingsgrunnlagService.opprettSøknad(behandling, null, null);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(SoeknadTrygdeavtale.class);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SØKNAD_TRYGDEAVTALE);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void opprettSøknad_behandleAlleSakerTrueTomFlytSkalIkkeLageBehGrunnlag_behGrunnlagBlirIkkeOpprettet() {
        fakeUnleash.enable("melosys.behandle_alle_saker");
        Behandling behandling = lagBehandling(Sakstyper.FTRL, Behandlingstema.YRKESAKTIV, Behandlingstyper.FØRSTEGANG);

        behandlingsgrunnlagService.opprettSøknad(behandling, null, null);

        verifyNoInteractions(behandlingService);
        verifyNoInteractions(behandlingsgrunnlagRepository);
    }

    @Test
    void opprettSøknad_behandleAlleSakerFalse_behGrunnlagBlirOpprettet() {
        fakeUnleash.enable("melosys.behandle_alle_saker");
        Behandling behandling = lagBehandling(Sakstyper.EU_EOS, Behandlingstema.YRKESAKTIV, Behandlingstyper.FØRSTEGANG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());

        behandlingsgrunnlagService.opprettSøknad(behandling, null, null);

        verify(behandlingsgrunnlagRepository).save(behandlingsgrunnlagArgumentCaptor.capture());
        Behandlingsgrunnlag opprettet = behandlingsgrunnlagArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getBehandlingsgrunnlagdata()).isInstanceOf(Soeknad.class);
        assertThat(opprettet.getType()).isEqualTo(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    private Behandling lagBehandling(Sakstyper sakstype, Behandlingstema tema) {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak(sakstype));
        behandling.setId(behandlingID);
        behandling.setInitierendeJournalpostId("123321");
        behandling.setTema(tema);
        return behandling;
    }

    private Behandling lagBehandling(Sakstyper sakstyper, Behandlingstema tema, Behandlingstyper behandlingstyper){
        Behandling behandling = lagBehandling(sakstyper, tema);
        behandling.setType(behandlingstyper);
        return behandling;
    }

    private Fagsak lagFagsak(Sakstyper sakstype) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstype);
        return fagsak;
    }
}
