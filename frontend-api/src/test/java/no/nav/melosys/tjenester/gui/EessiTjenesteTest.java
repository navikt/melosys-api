package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucerTilknyttetBehandlingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.OpprettBucSvarDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EessiTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(EessiTjenesteTest.class);

    private static final String MOTTAKERINSTITUSJONER_SCHEMA = "eessi-mottakerinstitusjoner-schema.json";
    private static final String OPPRETT_BUC_SCHEMA = "eessi-bucer-post-schema.json";
    private static final String BUCER_UNDER_ARBEID_SCHEMA = "eessi-bucer-schema.json";

    private static final String MOCK_RINA_URL = "http://rina-url.local/";

    @Mock
    private EessiService eessiService;
    @Mock
    private BehandlingService behandlingService;

    private EessiTjeneste eessiTjeneste;

    @BeforeEach
    void setup() {
        eessiTjeneste = new EessiTjeneste(eessiService, behandlingService);
    }

    @Test
    void hentMottakerInstitusjoner() throws IOException {
        when(eessiService.hentEessiMottakerinstitusjoner(anyString(), anyList()))
            .thenReturn(Arrays.asList(
                new Institusjon("1","Test1","NO"),
                new Institusjon("2","Test2","NO"),
                new Institusjon("3","Test3","NO")
            ));

        ResponseEntity<List<Institusjon>> response = eessiTjeneste.hentMottakerinstitusjoner("LA_BUC_01", List.of("SE"));
        assertThat(response.getBody()).hasOnlyElementsOfType(Institusjon.class);

        List<Institusjon> institusjoner = response.getBody();
        assertThat(institusjoner).isNotEmpty();
        validerArray(institusjoner, MOTTAKERINSTITUSJONER_SCHEMA, log);
    }

    @Test
    void opprettBuc() throws IOException {
        when(behandlingService.hentBehandling(123L)).thenReturn(lagBehandling());
        when(eessiService.opprettBucOgSed(any(), any(BucType.class), anyList(), anyCollection())).thenReturn(MOCK_RINA_URL);

        BucBestillingDto nyBucDto = new BucBestillingDto(
            BucType.LA_BUC_01,
            List.of("NAVT002"),
            defaultEasyRandom().objects(VedleggDto.class, 3).collect(Collectors.toSet())
        );
        ResponseEntity<OpprettBucSvarDto> response = eessiTjeneste.opprettBuc(nyBucDto, 123L);
        OpprettBucSvarDto opprettBucSvarDto = response.getBody();

        valider(nyBucDto, OPPRETT_BUC_SCHEMA, log);
        assertThat(opprettBucSvarDto).isNotNull()
            .extracting(OpprettBucSvarDto::getRinaUrl).isEqualTo(MOCK_RINA_URL);
        verify(eessiService).opprettBucOgSed(any(), eq(BucType.LA_BUC_01), anyList(), anyCollection());
    }

    @Test
    void hentBucer() throws IOException {
        when(behandlingService.hentBehandlingUtenSaksopplysninger(123L)).thenReturn(lagBehandling());
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList()))
            .thenReturn(Arrays.asList(
                bucInformasjon(),
                bucInformasjon(),
                bucInformasjon()
            ));

        ResponseEntity<BucerTilknyttetBehandlingDto> response = eessiTjeneste.hentBucer(123L, Arrays.asList("utkast", "sendt"));

        BucerTilknyttetBehandlingDto dto = response.getBody();
        assertThat(dto).extracting(BucerTilknyttetBehandlingDto::getBucer).hasNoNullFieldsOrProperties();

        valider(dto, BUCER_UNDER_ARBEID_SCHEMA, log);
    }


    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("321");
        fagsak.setGsakSaksnummer(123L);
        behandling.setFagsak(fagsak);
        return behandling;
    }

    private BucInformasjon bucInformasjon() {
        return new BucInformasjon(
            defaultEasyRandom().nextObject(String.class),
            true,
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(LocalDate.class),
            Collections.singleton(defaultEasyRandom().toString()),
            Arrays.asList(
                sedInformasjonMedGyldigUrl(),
                sedInformasjonMedGyldigUrl()
            )
        );
    }

    private SedInformasjon sedInformasjonMedGyldigUrl() {
        return new SedInformasjon(
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(LocalDate.class),
            defaultEasyRandom().nextObject(LocalDate.class),
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(String.class),
            MOCK_RINA_URL
        );
    }

    private static Journalpost lagJournalpost(String journalpostID, List<ArkivDokument> dokumenter) {
        Journalpost journalpost = new Journalpost(journalpostID);
        journalpost.setHoveddokument(dokumenter.get(0));
        journalpost.getVedleggListe().clear();
        journalpost.getVedleggListe().addAll(dokumenter.subList(1, dokumenter.size()));
        return journalpost;
    }

    private static ArkivDokument lagArkivDokument(String dokumentID) {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokumentID);
        arkivDokument.setTittel(dokumentID);
        return arkivDokument;
    }
}
