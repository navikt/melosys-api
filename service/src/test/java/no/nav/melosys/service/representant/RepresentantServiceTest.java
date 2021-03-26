package no.nav.melosys.service.representant;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.avgiftoverforing.AvgiftOverforingConsumer;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.representant.dto.RepresentantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepresentantServiceTest {
    @Mock
    private AvgiftOverforingConsumer avgiftOverforingConsumer;
    @Mock
    private MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    @Mock
    private AktoerRepository aktoerRepository;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private KontaktopplysningService kontaktopplysningService;

    @Captor
    private ArgumentCaptor<MedlemAvFolketrygden> medlemAvFolketrygdenArgumentCaptor;

    @Captor
    private ArgumentCaptor<Aktoer> aktoerArgumentCaptor;

    private RepresentantService representantService;

    @BeforeEach
    void setup(){
        representantService = new RepresentantService(avgiftOverforingConsumer, medlemAvFolketrygdenRepository, aktoerRepository, behandlingService, kontaktopplysningService);
    }

    @Test
    void hentRepresentantListe() {
        when(avgiftOverforingConsumer.hentRepresentantListe()).thenReturn(
            List.of(new AvgiftOverforingRepresentantDto("id1", "navn1"),
                new AvgiftOverforingRepresentantDto("id2", "navn2")));

        var representantDtoList = representantService.hentRepresentantListe();

        assertThat(representantDtoList).flatExtracting(RepresentantDto::nummer, RepresentantDto::navn)
            .containsExactly("id1", "navn1", "id2", "navn2");
    }

    @Test
    void hentRepresentant() {
        final var ID = "ID";
        when(avgiftOverforingConsumer.hentRepresentant(ID)).thenReturn(
            new AvgiftOverforingRepresentantDataDto(ID, "navn", List.of("adresselinje1", "adresselinje2"),
                "postnummer", "telefon", "123456789", "endretAv", LocalDate.now()));

        final var representantDataDto = representantService.hentRepresentant(ID);

        assertThat(representantDataDto.nummer()).isEqualTo(ID);
        assertThat(representantDataDto.navn()).isEqualTo("navn");
        assertThat(representantDataDto.adresselinjer()).containsExactly("adresselinje1", "adresselinje2");
        assertThat(representantDataDto.postnummer()).isEqualTo("postnummer");
        assertThat(representantDataDto.orgnr()).isEqualTo("123456789");
    }

    @Test
    void oppdaterValgtRepresentant_selvbetalendeIngenTidligereInfo_lagresKorrekt() throws FunksjonellException {
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(new FastsattTrygdeavgift());
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        representantService.oppdaterValgtRepresentant(1L, new ValgtRepresentant("repnr", true, null, null));

        verify(medlemAvFolketrygdenRepository, times(1)).save(medlemAvFolketrygdenArgumentCaptor.capture());
        verify(aktoerRepository).save(aktoerArgumentCaptor.capture());
        verify(aktoerRepository, never()).deleteById(anyLong());
        verify(kontaktopplysningService, never()).lagEllerOppdaterKontaktopplysning(anyString(), anyString(), anyString(), anyString(), anyString());

        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getRepresentantNr()).isEqualTo("repnr");
        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getBetalesAv()).isNull();

        assertThat(aktoerArgumentCaptor.getValue().getRolle()).isEqualTo(Aktoersroller.BRUKER);
        assertThat(aktoerArgumentCaptor.getValue().getFagsak()).isEqualTo(fagsak);
        assertThat(aktoerArgumentCaptor.getValue().getOrgnr()).isNull();
    }

    @Test
    void oppdaterValgtRepresentant_selvbetalendeTidligereInfo_lagresKorrekt() throws FunksjonellException {
        var aktoer = new Aktoer();
        aktoer.setId(2L);
        var fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setBetalesAv(aktoer);
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        fagsak.getAktører().add(aktoer);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        representantService.oppdaterValgtRepresentant(1L, new ValgtRepresentant("repnr", true, null, null));

        verify(medlemAvFolketrygdenRepository, times(1)).save(medlemAvFolketrygdenArgumentCaptor.capture());
        verify(aktoerRepository).save(aktoerArgumentCaptor.capture());
        verify(aktoerRepository, times(1)).deleteById(2L);
        verify(kontaktopplysningService, never()).lagEllerOppdaterKontaktopplysning(anyString(), anyString(), anyString(), anyString(), anyString());

        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getRepresentantNr()).isEqualTo("repnr");
        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getBetalesAv()).isNull();

        assertThat(aktoerArgumentCaptor.getValue().getRolle()).isEqualTo(Aktoersroller.BRUKER);
        assertThat(aktoerArgumentCaptor.getValue().getFagsak()).isEqualTo(fagsak);
        assertThat(aktoerArgumentCaptor.getValue().getOrgnr()).isNull();
    }

    @Test
    void oppdaterValgtRepresentant_ikkeSelvbetalendeIkkeTidligereInfo_lagresKorrekt() throws FunksjonellException {
        var saksnummerCaptor = ArgumentCaptor.forClass(String.class);
        var orgnrCaptor = ArgumentCaptor.forClass(String.class);
        var kontaktNavnCaptor = ArgumentCaptor.forClass(String.class);

        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(new FastsattTrygdeavgift());
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        representantService.oppdaterValgtRepresentant(1L, new ValgtRepresentant("repnr", false, "orgnr", "kontaktperson"));

        verify(medlemAvFolketrygdenRepository, times(1)).save(medlemAvFolketrygdenArgumentCaptor.capture());
        verify(aktoerRepository, never()).deleteById(anyLong());
        verify(aktoerRepository).save(aktoerArgumentCaptor.capture());
        verify(kontaktopplysningService, times(1)).lagEllerOppdaterKontaktopplysning(saksnummerCaptor.capture(), orgnrCaptor.capture(), isNull(), kontaktNavnCaptor.capture(), isNull());

        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getRepresentantNr()).isEqualTo("repnr");
        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getBetalesAv()).isNull();
        assertThat(aktoerArgumentCaptor.getValue().getFagsak()).isEqualTo(fagsak);
        assertThat(aktoerArgumentCaptor.getValue().getOrgnr()).isEqualTo("orgnr");
        assertThat(aktoerArgumentCaptor.getValue().getRolle()).isEqualTo(Aktoersroller.REPRESENTANT_TRYGDEAVGIFT);
        assertThat(aktoerArgumentCaptor.getValue().getId()).isNull();
        assertThat(saksnummerCaptor.getValue()).isEqualTo("saknr");
        assertThat(orgnrCaptor.getValue()).isEqualTo("orgnr");
        assertThat(kontaktNavnCaptor.getValue()).isEqualTo("kontaktperson");
    }

    @Test
    void oppdaterValgtRepresentant_ikkeSelvbetalendeTidligereInfo_lagresKorrekt() throws FunksjonellException {
        var saksnummerCaptor = ArgumentCaptor.forClass(String.class);
        var orgnrCaptor = ArgumentCaptor.forClass(String.class);
        var kontaktNavnCaptor = ArgumentCaptor.forClass(String.class);

        var aktoer = new Aktoer();
        aktoer.setId(2L);
        aktoer.setRolle(Aktoersroller.BRUKER);

        var fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setBetalesAv(aktoer);
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        fagsak.getAktører().add(aktoer);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        representantService.oppdaterValgtRepresentant(1L, new ValgtRepresentant("repnr", false, "orgnr", "kontaktperson"));

        verify(medlemAvFolketrygdenRepository, times(1)).save(medlemAvFolketrygdenArgumentCaptor.capture());
        verify(aktoerRepository, times(1)).deleteById(anyLong());
        verify(aktoerRepository, times(1)).save(aktoerArgumentCaptor.capture());
        verify(kontaktopplysningService, times(1)).lagEllerOppdaterKontaktopplysning(saksnummerCaptor.capture(), orgnrCaptor.capture(), isNull(), kontaktNavnCaptor.capture(), isNull());

        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getRepresentantNr()).isEqualTo("repnr");
        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getBetalesAv()).isNull();
        assertThat(aktoerArgumentCaptor.getValue().getFagsak()).isEqualTo(fagsak);
        assertThat(aktoerArgumentCaptor.getValue().getOrgnr()).isEqualTo("orgnr");
        assertThat(aktoerArgumentCaptor.getValue().getRolle()).isEqualTo(Aktoersroller.REPRESENTANT_TRYGDEAVGIFT);
        assertThat(aktoerArgumentCaptor.getValue().getId()).isNull();
        assertThat(saksnummerCaptor.getValue()).isEqualTo("saknr");
        assertThat(orgnrCaptor.getValue()).isEqualTo("orgnr");
        assertThat(kontaktNavnCaptor.getValue()).isEqualTo("kontaktperson");
    }

    @Test
    void oppdaterValgtRepresentant_sammeBetalesAvRolle_lagresKorrekt() throws FunksjonellException {
        var saksnummerCaptor = ArgumentCaptor.forClass(String.class);
        var orgnrCaptor = ArgumentCaptor.forClass(String.class);
        var kontaktNavnCaptor = ArgumentCaptor.forClass(String.class);

        var aktoer = new Aktoer();
        aktoer.setId(2L);
        aktoer.setRolle(Aktoersroller.REPRESENTANT_TRYGDEAVGIFT);

        var fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setBetalesAv(aktoer);
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        fagsak.getAktører().add(aktoer);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        representantService.oppdaterValgtRepresentant(1L, new ValgtRepresentant("repnr", false, "orgnr", "kontaktperson"));

        verify(medlemAvFolketrygdenRepository, times(1)).save(medlemAvFolketrygdenArgumentCaptor.capture());
        verify(aktoerRepository, never()).deleteById(anyLong());
        verify(aktoerRepository, times(1)).save(aktoerArgumentCaptor.capture());
        verify(kontaktopplysningService, times(1)).lagEllerOppdaterKontaktopplysning(saksnummerCaptor.capture(), orgnrCaptor.capture(), isNull(), kontaktNavnCaptor.capture(), isNull());

        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getRepresentantNr()).isEqualTo("repnr");
        assertThat(medlemAvFolketrygdenArgumentCaptor.getValue().getFastsattTrygdeavgift().getBetalesAv()).isNull();
        assertThat(aktoerArgumentCaptor.getValue().getFagsak()).isEqualTo(fagsak);
        assertThat(aktoerArgumentCaptor.getValue().getOrgnr()).isEqualTo("orgnr");
        assertThat(aktoerArgumentCaptor.getValue().getRolle()).isEqualTo(Aktoersroller.REPRESENTANT_TRYGDEAVGIFT);
        assertThat(aktoerArgumentCaptor.getValue().getId()).isEqualTo(2L);
        assertThat(saksnummerCaptor.getValue()).isEqualTo("saknr");
        assertThat(orgnrCaptor.getValue()).isEqualTo("orgnr");
        assertThat(kontaktNavnCaptor.getValue()).isEqualTo("kontaktperson");
    }

    @Test
    void oppdaterValgtRepresentant_manglerRepnr_kasterFeilmelding() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> representantService.oppdaterValgtRepresentant(1L,
                new ValgtRepresentant(null, false, "orgnr", "kontaktperson")))
            .withMessage("Representantnummer må være utfylt");
    }

    @Test
    void oppdaterValgtRepresentant_manglerOrgnr_kasterFeilmelding() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> representantService.oppdaterValgtRepresentant(1L,
                new ValgtRepresentant("repnr", false, null, "kontaktperson")))
            .withMessage("Når representant ikke er selvbetalende, må organisasjonsnummer være utfylt");
    }

    @Test
    void hentValgtRepresentant_selvbetalende_hentesKorrekt() throws FunksjonellException {
        var fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setRepresentantNr("repnr");
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var response = representantService.hentValgtRepresentant(1L);

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo("repnr");
        assertThat(response.isSelvbetalende()).isTrue();
        assertThat(response.getOrgnr()).isNull();
        assertThat(response.getKontaktperson()).isNull();
    }

    @Test
    void hentValgtRepresentant_ikkeSelvbetalendeKontaktpersonSatt_hentesKorrekt() throws FunksjonellException {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        var aktoer = new Aktoer();
        aktoer.setOrgnr("orgnr");
        aktoer.setFagsak(fagsak);
        var fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setRepresentantNr("repnr");
        fastsattTrygdeavgift.setBetalesAv(aktoer);
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        var kontaktopplysninger = new Kontaktopplysning();
        kontaktopplysninger.setKontaktNavn("kontaktperson");
        when(kontaktopplysningService.hentKontaktopplysning("saknr", "orgnr")).thenReturn(java.util.Optional.of(kontaktopplysninger));

        var response = representantService.hentValgtRepresentant(1L);

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo("repnr");
        assertThat(response.isSelvbetalende()).isFalse();
        assertThat(response.getOrgnr()).isEqualTo("orgnr");
        assertThat(response.getKontaktperson()).isEqualTo("kontaktperson");
    }

    @Test
    void hentValgtRepresentant_ikkeSelvbetalendeKontaktpersonIkkeSatt_hentesKorrekt() throws FunksjonellException {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer("saknr");
        var aktoer = new Aktoer();
        aktoer.setOrgnr("orgnr");
        aktoer.setFagsak(fagsak);
        var fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setRepresentantNr("repnr");
        fastsattTrygdeavgift.setBetalesAv(aktoer);
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(java.util.Optional.of(medlemAvFolketrygden));

        when(kontaktopplysningService.hentKontaktopplysning("saknr", "orgnr")).thenReturn(Optional.empty());

        var response = representantService.hentValgtRepresentant(1L);

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo("repnr");
        assertThat(response.isSelvbetalende()).isFalse();
        assertThat(response.getOrgnr()).isEqualTo("orgnr");
        assertThat(response.getKontaktperson()).isNull();
    }
}
