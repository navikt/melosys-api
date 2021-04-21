package no.nav.melosys.tjenester.gui;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
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
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import no.nav.melosys.tjenester.gui.dto.brev.FeltvalgDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevbestillingTjenesteTest extends JsonSchemaTestParent {

    @Mock
    private BehandlingService mockBehandlingService;
    @Mock
    private DokumentServiceFasade mockDokServiceFasade;
    @Mock
    private DokgenService mockDokgenService;
    @Mock
    private BrevmottakerService mockBrevmottakerService;
    @Mock
    private PersondataFasade mockPersondataFasade;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private KontaktopplysningService mockKontaktopplysningService;

    private BrevbestillingTjeneste brevbestillingTjeneste;

    @BeforeEach
    void init() {
        BrevmottakerService brevmottakerService = new BrevmottakerService(mockKontaktopplysningService, mock(AvklarteVirksomheterService.class), mock(UtenlandskMyndighetService.class), mock(BehandlingsresultatService.class), mock(TrygdeavgiftsberegningService.class));
        BrevbestillingService brevbestillingService = new BrevbestillingService(mockDokServiceFasade, mockDokgenService, mockBrevmottakerService, mockPersondataFasade, mockEregFasade, mockKontaktopplysningService, mock(KodeverkService.class));
        brevbestillingTjeneste = new BrevbestillingTjeneste(brevbestillingService, mockBehandlingService, brevmottakerService);
    }

    @Test
    void skalReturnereTilgjengeligeBrevmaler() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(2);

        assertThat(brevmaler.get(0).getType()).isEqualTo(MANGELBREV_BRUKER);
        assertThat(brevmaler.get(0).getFelter()).hasSize(2);
        assertThat(brevmaler.get(0).getFelter().get(0).getValg()).hasSize(1);
        assertThat(brevmaler.get(0).getMuligeMottakere()).hasSize(1);

        assertThat(brevmaler.get(1).getType()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(brevmaler.get(1).getFelter().get(0).getValg()).hasSize(1);
        assertThat(brevmaler.get(1).getMuligeMottakere()).hasSize(2);
    }

    @Test
    void hentTilgjengeligeMaler_soeknad_returnererSoeknadMalOgEndredeValg() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Behandlingstyper.SOEKNAD));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(3);

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


    }

    @Test
    void hentTilgjengeligeMaler_brukerAdresseNull_returnererMalMedFeilmelding() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(Collections.emptyList());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(2);
        assertThat(brevmaler.get(0).getMuligeMottakere().get(0).getFeilmelding())
            .isEqualTo("Bruker har ingen registrert adresse.");
    }

    @Test
    void hentTilgjengeligeMaler_registerOpplysningerIkkeHentet_returnererMalMedFeilmelding() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(null));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any(), anyBoolean(), anyBoolean()))
            .thenThrow(new TekniskException("Finner ikke arbeidsforholddokument"));

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertThat(brevmaler).hasSize(2);
        assertThat(brevmaler.get(1).getMuligeMottakere().get(0).getFeilmelding())
            .isEqualTo("Finner ingen arbeidsgivere. Hent registeropplysninger.");
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

        verifyNoInteractions(mockDokgenService);

        valider(brevbestillingDto, "dokumenter-v2-utkast-post-schema.json", new ObjectMapper());
    }

    @Test
    void skalBestilleProduseringAvBrev() throws Exception {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medMottaker(Aktoersroller.BRUKER)
            .medInnledningFritekst("Innledning")
            .medManglerFritekst("Mangler")
            .build();
        brevbestillingTjeneste.produserBrev(123L, brevbestillingDto);

        verify(mockDokgenService).produserOgDistribuerBrev(123L, brevbestillingDto);
        verifyNoInteractions(mockDokServiceFasade);

        valider(brevbestillingDto, "dokumenter-v2-opprett-post-schema.json", new ObjectMapper());
    }

    private Behandling lagBehandling(Behandlingstyper type) {
        var behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(new Fagsak());
        behandling.setType(type);
        return behandling;
    }
}