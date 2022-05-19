package no.nav.melosys.tjenester.gui;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.tjenester.gui.FeltvalgAlternativKode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevbestillingTjenesteTest extends JsonSchemaTestParent {

    public static final String SAKSBEHANDLER = "Z123456";
    @Mock
    private BehandlingService mockBehandlingService;
    @Mock
    private DokumentServiceFasade mockDokServiceFasade;
    @Mock
    private BrevmottakerService mockBrevmottakerService;
    @Mock
    private PersondataFasade mockPersondataFasade;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private KontaktopplysningService mockKontaktopplysningService;
    @Mock
    private Aksesskontroll aksesskontroll;
    @Mock
    private DokumentNavnService mockDokumentNavnService;
    @Captor
    private ArgumentCaptor<BrevbestillingRequest> brevbestillingDtoCaptor;

    private BrevbestillingTjeneste brevbestillingTjeneste;

    @BeforeEach
    void init() {
        BrevmottakerService brevmottakerService = new BrevmottakerService(mockKontaktopplysningService,
            mock(AvklarteVirksomheterService.class), mock(UtenlandskMyndighetService.class), mock(BehandlingsresultatService.class),
            mock(TrygdeavgiftsberegningService.class), mock(LovvalgsperiodeService.class), mockBehandlingService);
        BrevbestillingService brevbestillingService = new BrevbestillingService(mockBrevmottakerService,
            mockDokServiceFasade, mockBehandlingService, mockEregFasade, mockKontaktopplysningService,
            mockPersondataFasade, mockDokumentNavnService);
        brevbestillingTjeneste = new BrevbestillingTjeneste(brevbestillingService, mockBehandlingService, brevmottakerService, aksesskontroll);
    }

    @Test
    void skalReturnereTilgjengeligeBrevmaler() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(null));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(4);

        assertThat(brevmaler.get(0).getType()).isEqualTo(MANGELBREV_BRUKER);
        assertThat(brevmaler.get(0).getFelter()).hasSize(2);
        assertThat(brevmaler.get(0).getFelter().get(0).getValg().getValgAlternativer()).hasSize(1);

        assertThat(brevmaler.get(1).getType()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(brevmaler.get(1).getFelter().get(0).getValg().getValgAlternativer()).hasSize(1);

        assertThat(brevmaler.get(2).getType()).isEqualTo(GENERELT_FRITEKSTBREV_BRUKER);

        assertThat(brevmaler.get(3).getType()).isEqualTo(GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
    }

    @Test
    void hentTilgjengeligeMaler_soeknad_returnererSoeknadMalMedFelter() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(5);

        assertThat(brevmaler.get(0).getType()).isEqualTo(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        assertThat(brevmaler.get(0).getFelter()).isNull();
        assertThat(brevmaler.get(0).getMuligeMottakere()).hasSize(1);

        assertThat(brevmaler.get(1).getFelter()).hasSize(2);
        assertThat(brevmaler.get(1).getType()).isEqualTo(MANGELBREV_BRUKER);

        assertThat(brevmaler.get(2).getFelter()).hasSize(2);
        assertThat(brevmaler.get(2).getType()).isEqualTo(MANGELBREV_ARBEIDSGIVER);

        assertThat(brevmaler.get(3).getFelter()).hasSize(3);
        assertThat(brevmaler.get(3).getType()).isEqualTo(GENERELT_FRITEKSTBREV_BRUKER);

        assertThat(brevmaler.get(4).getFelter()).hasSize(3);
        assertThat(brevmaler.get(4).getType()).isEqualTo(GENERELT_FRITEKSTBREV_ARBEIDSGIVER);

    }

    @Test
    void hentTilgjengeligeMaler_brukerAdresseNull_returnererMalMedFeilmelding() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(null));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(4);
        assertThat(brevmaler.get(0).getMuligeMottakere().get(0).getFeilmelding())
            .isEqualTo("Bruker har ingen registrert adresse.");
    }

    @Test
    void hentTilgjengeligeMaler_registerOpplysningerIkkeHentet_returnererMalMedFeilmelding() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(null));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean()))
            .thenThrow(new TekniskException("Finner ikke arbeidsforholddokument"));

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(4);
        assertThat(brevmaler.get(1).getMuligeMottakere().get(0).getFeilmelding())
            .isEqualTo("Finner ingen arbeidsgivere. Hent registeropplysninger.");
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeValgForMangelbrevForBruker() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler.get(1).getFelter()).hasSize(2);
        assertThat(brevmaler.get(1).getType()).isEqualTo(MANGELBREV_BRUKER);
        assertThat(brevmaler.get(1).getFelter().get(0).getValg().getValgAlternativer()).hasSize(2)
            .extracting(FeltvalgAlternativDto::getKode)
            .containsExactlyInAnyOrder(FRITEKST.getKode(), STANDARD.getKode());
        FeltValgDto mangelbrevBrukerFeltValg = brevmaler.get(1).getFelter().get(0).getValg();
        assertThat(mangelbrevBrukerFeltValg.getValgAlternativer().get(1).isVisFelt()).isTrue();
        assertThat(mangelbrevBrukerFeltValg.getValgAlternativer().get(1).getKode()).isEqualTo(FRITEKST.getKode());
        assertThat(mangelbrevBrukerFeltValg.getValgType()).isEqualTo(FeltValgType.RADIO);
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeValgForMangelbrevForArbeidsgiver() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler.get(2).getFelter()).hasSize(2);
        assertThat(brevmaler.get(2).getType()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(brevmaler.get(2).getFelter().get(0).getValg().getValgAlternativer()).hasSize(2)
            .extracting(FeltvalgAlternativDto::getKode)
            .containsExactlyInAnyOrder(FRITEKST.getKode(), STANDARD.getKode());
        FeltValgDto mangelbrevArbeidsgiverFeltValg = brevmaler.get(2).getFelter().get(0).getValg();
        assertThat(mangelbrevArbeidsgiverFeltValg.getValgAlternativer().get(1).isVisFelt()).isTrue();
        assertThat(mangelbrevArbeidsgiverFeltValg.getValgAlternativer().get(1).getKode()).isEqualTo(FRITEKST.getKode());
        assertThat(mangelbrevArbeidsgiverFeltValg.getValgType()).isEqualTo(FeltValgType.RADIO);
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeTittelValgForFritekstbrevForEuEOS() {
        Behandling behandlingEUEOS = lagBehandling(Behandlingstyper.SOEKNAD);
        behandlingEUEOS.getFagsak().setType(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingEUEOS);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingEUEOS);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler).hasSize(5);
        assertThat(brevmaler.get(3).getFelter().get(0).getValg().getValgAlternativer()).hasSize(2)
            .extracting(FeltvalgAlternativDto::getKode)
            .containsExactlyInAnyOrder(HENVENDELSE_OM_TRYGDETILHØRLIGHET.getKode(), FRITEKST.getKode());
        assertThat(brevmaler.get(3).getFelter().get(0).getValg().getValgAlternativer()).hasSize(2)
            .extracting(FeltvalgAlternativDto::isVisFelt)
            .containsExactlyInAnyOrder(false, true);
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeTittelValgForFritekstbrevForFTRL() {
        Behandling behandlingFTRL = lagBehandling(Behandlingstyper.SOEKNAD);
        behandlingFTRL.getFagsak().setType(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingFTRL);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingFTRL);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler).hasSize(5);
        assertThat(brevmaler.get(4).getFelter().get(0).getValg().getValgAlternativer()).hasSize(4)
            .extracting(FeltvalgAlternativDto::getKode)
            .containsExactlyInAnyOrder(HENVENDELSE_OM_MEDLEMSKAP.getKode(),
                BEKREFTELSE_PÅ_MEDLEMSKAP.getKode(),
                CONFIRMATION_OF_MEMBERSHIP.getKode(),
                FRITEKST.getKode());
        assertThat(brevmaler.get(4).getFelter().get(0).getValg().getValgAlternativer()).hasSize(4)
            .extracting(FeltvalgAlternativDto::isVisFelt)
            .containsExactlyInAnyOrder(false, false, false, true);
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeTittelValgForFritekstbrevForTrygdeavtale() {
        Behandling behandlingTrygdeavtale = lagBehandling(Behandlingstyper.SOEKNAD);
        behandlingTrygdeavtale.getFagsak().setType(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandlingTrygdeavtale);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandlingTrygdeavtale);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler).hasSize(5);
        assertThat(brevmaler.get(4).getFelter().get(0).getValg().getValgAlternativer()).hasSize(2)
            .extracting(FeltvalgAlternativDto::getKode)
            .containsExactlyInAnyOrder(HENVENDELSE_OM_MEDLEMSKAP.getKode(),
                FRITEKST.getKode());
        assertThat(brevmaler.get(4).getFelter().get(0).getValg().getValgAlternativer()).hasSize(2)
            .extracting(FeltvalgAlternativDto::isVisFelt)
            .containsExactlyInAnyOrder(false, true);
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeMottakereForArbeidsgiver() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        List<Produserbaredokumenter> arbeidsgiverBrev = List.of(GENERELT_FRITEKSTBREV_ARBEIDSGIVER, MANGELBREV_ARBEIDSGIVER);
        brevmaler.stream().filter(brevmal -> arbeidsgiverBrev.contains(brevmal.getType()))
            .forEach(brevmalDto ->
                assertThat(brevmalDto.getMuligeMottakere())
                    .extracting(MottakerDto::getType)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("Arbeidsgiver eller arbeidsgivers fullmektig", "Annen organisasjon"));
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeMottakereForVirksomhet() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        List<Produserbaredokumenter> arbeidsgiverBrev = List.of(GENERELT_FRITEKSTBREV_VIRKSOMHET);
        brevmaler.stream().filter(brevmal -> arbeidsgiverBrev.contains(brevmal.getType()))
            .forEach(brevmalDto ->
                assertThat(brevmalDto.getMuligeMottakere())
                    .extracting(MottakerDto::getType)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("Virksomhet", "Annen organisasjon"));
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeMottakerTilBrukerBehandlingstypeSoknad() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        List<Produserbaredokumenter> arbeidsgiverBrev = Arrays.asList(
            GENERELT_FRITEKSTBREV_BRUKER,
            MANGELBREV_BRUKER,
            MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
        );
        brevmaler.stream().filter(brevmal -> arbeidsgiverBrev.contains(brevmal.getType()))
            .forEach(brevmalDto ->
                assertThat(brevmalDto.getMuligeMottakere())
                    .extracting(MottakerDto::getType)
                    .hasSize(1)
                    .containsExactly("Bruker eller brukers fullmektig"));
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeMottakerTilBrukerBehandlingstypeKlage() {
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling(Behandlingstyper.KLAGE));
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.KLAGE));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler.get(0)).hasFieldOrPropertyWithValue("type", MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE);
        assertThat(brevmaler.get(0).getMuligeMottakere()).extracting(MottakerDto::getType)
            .containsExactly("Bruker eller brukers fullmektig");
    }

    @Test
    void skalReturnereUtkast() throws Exception {
        byte[] forventetPdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(mockDokServiceFasade.produserUtkast(anyLong(), any())).thenReturn(forventetPdf);

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medMottaker(Aktoersroller.BRUKER)
            .medInnledningFritekst("Innledning")
            .medManglerFritekst("Mangler")
            .build();
        ResponseEntity<byte[]> responseEntity = brevbestillingTjeneste.produserUtkast(123L, brevbestillingDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(forventetPdf);

        valider(brevbestillingDto, "dokumenter-v2-utkast-post-schema.json", new ObjectMapper());
    }

    @Test
    void skalBestilleProduseringAvBrev() throws Exception {
        settInnloggetSaksbehandler();

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medMottaker(Aktoersroller.BRUKER)
            .medInnledningFritekst("Innledning")
            .medManglerFritekst("Mangler")
            .build();
        brevbestillingTjeneste.produserBrev(123L, brevbestillingDto);

        verify(mockDokServiceFasade).produserDokument(anyLong(), brevbestillingDtoCaptor.capture());

        assertThat(brevbestillingDtoCaptor.getValue()).extracting(
            BrevbestillingRequest::getProduserbardokument,
            BrevbestillingRequest::getMottaker,
            BrevbestillingRequest::getInnledningFritekst,
            BrevbestillingRequest::getBestillersId
        ).containsExactly(MANGELBREV_BRUKER, Aktoersroller.BRUKER, "Innledning", SAKSBEHANDLER);

        valider(brevbestillingDto, "dokumenter-v2-opprett-post-schema.json", new ObjectMapper());
    }

    @Test
    void skalBestilleProduseringAvFritekstbrev() throws Exception {
        settInnloggetSaksbehandler();

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medMottaker(Aktoersroller.BRUKER)
            .medFritekstTittel("Tittel")
            .medFritekst("Innhold")
            .build();
        brevbestillingTjeneste.produserBrev(123L, brevbestillingDto);

        verify(mockDokServiceFasade).produserDokument(anyLong(), brevbestillingDtoCaptor.capture());

        assertThat(brevbestillingDtoCaptor.getValue()).extracting(
            BrevbestillingRequest::getProduserbardokument,
            BrevbestillingRequest::getMottaker,
            BrevbestillingRequest::getFritekstTittel,
            BrevbestillingRequest::getFritekst
        ).containsExactly(GENERELT_FRITEKSTBREV_BRUKER, Aktoersroller.BRUKER, "Tittel", "Innhold");

        valider(brevbestillingDto, "dokumenter-v2-opprett-post-schema.json", new ObjectMapper());
    }

    private Behandling lagBehandling(Behandlingstyper type) {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setAktører(Set.of(bruker));
        var behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setType(type);
        return behandling;
    }

    private void settInnloggetSaksbehandler() {
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(SAKSBEHANDLER);
    }
}
