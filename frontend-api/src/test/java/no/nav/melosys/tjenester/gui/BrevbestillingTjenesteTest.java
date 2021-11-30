package no.nav.melosys.tjenester.gui;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.kodeverk.KodeverkService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Captor
    private ArgumentCaptor<BrevbestillingRequest> brevbestillingDtoCaptor;
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private BrevbestillingTjeneste brevbestillingTjeneste;

    @BeforeEach
    void init() {
        BrevmottakerService brevmottakerService = new BrevmottakerService(mockKontaktopplysningService, mock(AvklarteVirksomheterService.class), mock(UtenlandskMyndighetService.class), mock(BehandlingsresultatService.class), mock(TrygdeavgiftsberegningService.class));
        BrevbestillingService brevbestillingService = new BrevbestillingService(mockBrevmottakerService,
            mockDokServiceFasade, mockEregFasade, mock(KodeverkService.class), mockKontaktopplysningService,
            mockPersondataFasade, fakeUnleash);
        brevbestillingTjeneste = new BrevbestillingTjeneste(brevbestillingService, mockBehandlingService, brevmottakerService, aksesskontroll);
        fakeUnleash.enable("melosys.brev.GENERELT_FRITEKSTBREV_ARBEIDSGIVER");
        fakeUnleash.enable("melosys.brev.GENERELT_FRITEKSTBREV_BRUKER");
    }

    @Test
    void skalReturnereTilgjengeligeBrevmaler() {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(4);

        assertThat(brevmaler.get(0).getType()).isEqualTo(MANGELBREV_BRUKER);
        assertThat(brevmaler.get(0).getFelter()).hasSize(2);
        assertThat(brevmaler.get(0).getFelter().get(0).getValg()).hasSize(1);
        assertThat(brevmaler.get(0).getMuligeMottakere()).hasSize(1);

        assertThat(brevmaler.get(1).getType()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(brevmaler.get(1).getFelter().get(0).getValg()).hasSize(1);
        assertThat(brevmaler.get(1).getMuligeMottakere()).hasSize(2);

        assertThat(brevmaler.get(2).getType()).isEqualTo(GENERELT_FRITEKSTBREV_BRUKER);
        assertThat(brevmaler.get(2).getMuligeMottakere()).hasSize(1);

        assertThat(brevmaler.get(3).getType()).isEqualTo(GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
        assertThat(brevmaler.get(3).getMuligeMottakere()).hasSize(1);
    }

    @Test
    void hentTilgjengeligeMaler_soeknad_returnererSoeknadMalOgEndredeValg() {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(5);

        assertThat(brevmaler.get(0).getType()).isEqualTo(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        assertThat(brevmaler.get(0).getFelter()).isNull();
        assertThat(brevmaler.get(0).getMuligeMottakere()).hasSize(1);

        assertThat(brevmaler.get(1).getFelter()).hasSize(2);
        assertThat(brevmaler.get(1).getFelter().get(0).getValg()).hasSize(2)
            .extracting(FeltvalgDto::getKode)
            .containsExactlyInAnyOrder("FRITEKST", "STANDARD");

        assertThat(brevmaler.get(2).getFelter()).hasSize(2);
        assertThat(brevmaler.get(2).getFelter().get(0).getValg()).hasSize(2)
            .extracting(FeltvalgDto::getKode)
            .containsExactlyInAnyOrder("FRITEKST", "STANDARD");

        assertThat(brevmaler.get(3).getFelter()).hasSize(3);
        assertThat(brevmaler.get(3).getType()).isEqualTo(GENERELT_FRITEKSTBREV_BRUKER);

        assertThat(brevmaler.get(4).getFelter()).hasSize(3);
        assertThat(brevmaler.get(4).getType()).isEqualTo(GENERELT_FRITEKSTBREV_ARBEIDSGIVER);

    }

    @Test
    void hentTilgjengeligeMaler_brukerAdresseNull_returnererMalMedFeilmelding() {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(4);
        assertThat(brevmaler.get(0).getMuligeMottakere().get(0).getFeilmelding())
            .isEqualTo("Bruker har ingen registrert adresse.");
    }

    @Test
    void hentTilgjengeligeMaler_registerOpplysningerIkkeHentet_returnererMalMedFeilmelding() {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean()))
            .thenThrow(new TekniskException("Finner ikke arbeidsforholddokument"));

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(4);
        assertThat(brevmaler.get(1).getMuligeMottakere().get(0).getFeilmelding())
            .isEqualTo("Finner ingen arbeidsgivere. Hent registeropplysninger.");
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeTittelValgForFritekstbrevForEuEOS() {
        Behandling behandling_EU_EOS = lagBehandling(Behandlingstyper.SOEKNAD);
        behandling_EU_EOS.getFagsak().setType(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling_EU_EOS);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler).hasSize(5);
        assertThat(brevmaler.get(3).getFelter().get(0).getValg()).hasSize(2)
            .extracting(FeltvalgDto::getKode)
            .containsExactlyInAnyOrder("HENVENDELSE_OM_TRYGDETILHØRLIGHET", "FRITEKST");
    }

    @Test
    void hentTilgjengeligeMaler_lagerRiktigeTittelValgForFritekstbrevForFTRL() {
        Behandling behandling_FTRL = lagBehandling(Behandlingstyper.SOEKNAD);
        behandling_FTRL.getFagsak().setType(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling_FTRL);
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);

        assertThat(brevmaler).hasSize(5);
        assertThat(brevmaler.get(4).getFelter().get(0).getValg()).hasSize(4)
            .extracting(FeltvalgDto::getKode)
            .containsExactlyInAnyOrder("HENVENDELSE_OM_MEDLEMSKAP",
                "BEKREFTELSE_PÅ_MEDLEMSKAP",
                "CONFIRMATION_OF_MEMBERSHIP",
                "FRITEKST");
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
        var behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
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
