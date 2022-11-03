package no.nav.melosys.service.mottatteopplysninger;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.mottatteopplysninger.*;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.MottatteOpplysningerRepository;
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
class MottatteOpplysningerServiceTest {
    @Mock
    private MottatteOpplysningerRepository mottatteOpplysningerRepository;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private JoarkFasade joarkFasade;

    private MottatteOpplysningerService mottatteOpplysningerService;

    @Captor
    private ArgumentCaptor<MottatteOpplysninger> mottatteOpplysningerArgumentCaptor;

    private final long behandlingID = 123332211;

    private final FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    public void setup() {
        mottatteOpplysningerService = new MottatteOpplysningerService(mottatteOpplysningerRepository, behandlingService, joarkFasade, unleash);

        unleash.enableAll();
    }

    @Test
    void hentMottatteOpplysningerForBehandlingID_finnes_returnerMottatteOpplysninger() {
        when(mottatteOpplysningerRepository.findByBehandling_Id(behandlingID))
            .thenReturn(Optional.of(new MottatteOpplysninger()));
        assertThat(mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID)).isNotNull();
    }

    @Test
    void hentMottatteOpplysningerForBehandlingID_finnesIkke_kastException() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> mottatteOpplysningerService.hentMottatteOpplysninger(1))
            .withMessageContaining("Finner ikke mottatteOpplysninger for behandling 1");
    }

    @Test
    void opprettEøsSøknadGrunnlag_finnesIkkeFraFør_blirOpprettet() {
        Behandling behandling = lagBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());
        Periode periode = new Periode();
        Soeknadsland soeknadsland = new Soeknadsland();


        mottatteOpplysningerService.opprettSøknad(behandling, periode, soeknadsland);


        verify(mottatteOpplysningerRepository).save(mottatteOpplysningerArgumentCaptor.capture());
        MottatteOpplysninger opprettet = mottatteOpplysningerArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getMottatteOpplysningerData()).isInstanceOf(Soeknad.class);
        assertThat(opprettet.getMottatteOpplysningerData().periode).isEqualTo(periode);
        assertThat(opprettet.getMottatteOpplysningerData().soeknadsland).isEqualTo(soeknadsland);
        assertThat(opprettet.getType()).isEqualTo(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void oppdaterMottatteOpplysningerJson_mottatteopplysningerEksisterer_oppdatererMottatteOpplysningerData() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        MottatteOpplysningerData originalData = new MottatteOpplysningerData();
        String originalJsonData = objectMapper.writeValueAsString(originalData);
        mottatteOpplysninger.setJsonData(originalJsonData);
        mottatteOpplysninger.setMottatteOpplysningerdata(new MottatteOpplysningerData());
        when(mottatteOpplysningerRepository.findByBehandling_Id(behandlingID)).thenReturn(Optional.of(mottatteOpplysninger));

        MottatteOpplysningerData nyData = new Soeknad();
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(nyData));

        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandlingID, jsonNode);
        verify(mottatteOpplysningerRepository).saveAndFlush(any(MottatteOpplysninger.class));

        assertThat(mottatteOpplysninger.getJsonData()).isNotEqualTo(originalJsonData);
    }

    @Test
    void oppdaterMottatteOpplysninger_mottatteopplysningerJsonDataIkkeSatt_setterJsonDataOgLagrerMottatteOpplysninger() throws JsonProcessingException {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.periode = new Periode(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2010, 1, 1)
        );
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData);

        mottatteOpplysningerService.oppdaterMottatteOpplysninger(mottatteOpplysninger);

        verify(mottatteOpplysningerRepository).saveAndFlush(mottatteOpplysningerArgumentCaptor.capture());
        JsonNode jsonNode = new ObjectMapper().readTree(mottatteOpplysningerArgumentCaptor.getValue().getJsonData());
        String periode = jsonNode.get("periode").toString();
        assertThat(periode)
            .isEqualTo("{" +
                "\"fom\":[2000,1,1]," +
                "\"tom\":[2010,1,1]" +
                "}");
    }

    @Test
    void oppdaterMottatteOpplysningerPeriodeOgLand_eksisterer_oppdatererPeriodeOgLand() {
        ArgumentCaptor<MottatteOpplysninger> captor = ArgumentCaptor.forClass(MottatteOpplysninger.class);
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(new MottatteOpplysningerData());

        when(mottatteOpplysningerRepository.findByBehandling_Id(behandlingID)).thenReturn(Optional.of(mottatteOpplysninger));

        var periode = new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31));
        var soeknadsland = new Soeknadsland(List.of("UK"), false);

        mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingID, periode, soeknadsland);

        verify(mottatteOpplysningerRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMottatteOpplysningerData().periode).isEqualTo(periode);
        assertThat(captor.getValue().getMottatteOpplysningerData().soeknadsland).isEqualTo(soeknadsland);
    }

    @Test
    void opprettSedGrunnlag_harRettType() {
        Behandling behandling = lagBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());
        SedGrunnlag sedGrunnlag = new SedGrunnlag();


        mottatteOpplysningerService.opprettSedGrunnlag(behandlingID, sedGrunnlag);


        verify(mottatteOpplysningerRepository).save(mottatteOpplysningerArgumentCaptor.capture());
        MottatteOpplysninger opprettet = mottatteOpplysningerArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getMottatteOpplysningerData()).isInstanceOf(SedGrunnlag.class);
        assertThat(opprettet.getType()).isEqualTo(Mottatteopplysningertyper.SED);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void opprettSøknadFolketrygden_harRettType() {
        Behandling behandling = lagBehandling(Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.ARBEID_I_UTLANDET);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());


        mottatteOpplysningerService.opprettSøknad(behandling, null, null);


        verify(mottatteOpplysningerRepository).save(mottatteOpplysningerArgumentCaptor.capture());
        MottatteOpplysninger opprettet = mottatteOpplysningerArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getMottatteOpplysningerData()).isInstanceOf(SoeknadFtrl.class);
        assertThat(opprettet.getType()).isEqualTo(Mottatteopplysningertyper.SØKNAD_FOLKETRYGDEN);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void opprettSøknadTrygdeavtale_harRettType() {
        Behandling behandling = lagBehandling(Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());


        mottatteOpplysningerService.opprettSøknad(behandling, null, null);

        verify(mottatteOpplysningerRepository).save(mottatteOpplysningerArgumentCaptor.capture());
        MottatteOpplysninger opprettet = mottatteOpplysningerArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getMottatteOpplysningerData()).isInstanceOf(SoeknadTrygdeavtale.class);
        assertThat(opprettet.getType()).isEqualTo(Mottatteopplysningertyper.SØKNAD_TRYGDEAVTALE);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    @Test
    void opprettSøknad_tomFlyt_mottatteOpplysningerBlirIkkeOpprettet() {
        Behandling behandling = lagBehandling(Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        mottatteOpplysningerService.opprettSøknad(behandling, null, null);

        verifyNoInteractions(behandlingService);
        verifyNoInteractions(mottatteOpplysningerRepository);
    }

    @Test
    void opprettSøknad_mottatteOpplysningerBlirOpprettet() {
        Behandling behandling = lagBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(joarkFasade.hentMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(LocalDate.now());

        mottatteOpplysningerService.opprettSøknad(behandling, null, null);

        verify(mottatteOpplysningerRepository).save(mottatteOpplysningerArgumentCaptor.capture());
        MottatteOpplysninger opprettet = mottatteOpplysningerArgumentCaptor.getValue();

        assertThat(opprettet).isNotNull();
        assertThat(opprettet.getMottatteOpplysningerData()).isInstanceOf(Soeknad.class);
        assertThat(opprettet.getType()).isEqualTo(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        assertThat(opprettet.getBehandling()).isEqualTo(behandling);
        assertThat(opprettet.getMottaksdato()).isNotNull();
    }

    private Behandling lagBehandling(Sakstyper sakstype, Sakstemaer sakstemaer, Behandlingstema tema) {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak(sakstype, sakstemaer));
        behandling.setId(behandlingID);
        behandling.setInitierendeJournalpostId("123321");
        behandling.setTema(tema);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        return behandling;
    }

    private Fagsak lagFagsak(Sakstyper sakstype, Sakstemaer sakstemaer) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstype);
        fagsak.setTema(sakstemaer);
        return fagsak;
    }
}
