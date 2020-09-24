package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucerTilknyttetBehandlingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.OpprettBucSvarDto;
import no.nav.melosys.tjenester.gui.dto.eessi.VedleggDto;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EessiTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private static final String MOTTAKERINSTITUSJONER_SCHEMA = "eessi-mottakerinstitusjoner-schema.json";
    private static final String OPPRETT_BUC_SCHEMA = "eessi-bucer-post-schema.json";
    private static final String BUCER_UNDER_ARBEID_SCHEMA = "eessi-bucer-schema.json";

    private static final String MOCK_RINA_URL = "http://rina-url.local/";

    @Mock
    private EessiService eessiService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private DokumentHentingService dokumentHentingService;

    @Captor
    private ArgumentCaptor<List<Vedlegg>> vedleggCaptor;

    private EessiTjeneste eessiTjeneste;

    @Before
    public void setup() throws IkkeFunnetException, SikkerhetsbegrensningException {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("321");
        fagsak.setGsakSaksnummer(123L);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(123L))).thenReturn(behandling);
        when(behandlingService.hentBehandling(eq(123L))).thenReturn(behandling);
        when(dokumentHentingService.hentDokument(anyString(), anyString())).thenReturn(new byte[0]);

        eessiTjeneste = new EessiTjeneste(eessiService, behandlingService, dokumentHentingService);
    }

    @Test
    public void hentMottakerInstitusjoner() throws IOException, MelosysException {
        when(eessiService.hentEessiMottakerinstitusjoner(anyString(), anyList()))
            .thenReturn(Arrays.asList(
                defaultEasyRandom().nextObject(Institusjon.class),
                defaultEasyRandom().nextObject(Institusjon.class),
                defaultEasyRandom().nextObject(Institusjon.class)
            ));

        ResponseEntity<List<Institusjon>> response = eessiTjeneste.hentMottakerinstitusjoner("LA_BUC_01", List.of("SE"));
        assertThat(response.getBody()).hasOnlyElementsOfType(Institusjon.class);

        List<Institusjon> institusjoner = response.getBody();
        assertThat(institusjoner).isNotEmpty();
        validerArray(institusjoner, MOTTAKERINSTITUSJONER_SCHEMA, log);
    }

    @Ignore // FIXME: Endre mottakerId til mottakerInstitusjoner i JSON-schema
    @Test
    public void opprettBuc() throws IOException, MelosysException {
        when(eessiService.opprettBucOgSed(any(), any(BucType.class), anyList(), anyList(), anyCollection())).thenReturn(MOCK_RINA_URL);

        BucBestillingDto nyBucDto = new BucBestillingDto(BucType.LA_BUC_01, List.of("NO"), List.of("NAVT002"), Collections.emptyList());
        ResponseEntity<OpprettBucSvarDto> response = eessiTjeneste.opprettBuc(nyBucDto, 123L);
        OpprettBucSvarDto opprettBucSvarDto = response.getBody();

        valider(nyBucDto, OPPRETT_BUC_SCHEMA, log);
        assertThat(opprettBucSvarDto).isNotNull()
            .extracting(OpprettBucSvarDto::getRinaUrl).isEqualTo(MOCK_RINA_URL);
    }

    @Test
    public void opprettBuc_medVedlegg_validerVedlegg() throws MelosysException {
        when(eessiService.opprettBucOgSed(any(), any(BucType.class), anyList(), anyList(), anyCollection())).thenReturn(MOCK_RINA_URL);

        List<Journalpost> journalposter = List.of(
            lagJournalpost("1",
                List.of(
                    lagArkivDokument("1"),
                    lagArkivDokument("2"),
                    lagArkivDokument("3")
                )),
            lagJournalpost("2",
                List.of(
                    lagArkivDokument("1")
                )),
            lagJournalpost("3",
                List.of(
                    lagArkivDokument("1")
                )));

        when(dokumentHentingService.hentDokumenter(eq("321"))).thenReturn(journalposter);

        List<VedleggDto> vedleggDtoList = List.of(
            new VedleggDto("1", "1"),
            new VedleggDto("1", "2"),
            new VedleggDto("1", "3"),
            new VedleggDto("2", "1"),
            new VedleggDto("3", "1")
        );

        BucBestillingDto nyBucDto = new BucBestillingDto(BucType.LA_BUC_01, List.of("NO"), List.of("NAVT002"), vedleggDtoList);
        ResponseEntity<OpprettBucSvarDto> response = eessiTjeneste.opprettBuc(nyBucDto, 123L);
        OpprettBucSvarDto opprettBucSvarDto = response.getBody();

        assertThat(opprettBucSvarDto).isNotNull()
            .extracting(OpprettBucSvarDto::getRinaUrl).isEqualTo(MOCK_RINA_URL);

        verify(dokumentHentingService, times(3)).hentDokument(eq("1"), anyString());
        verify(dokumentHentingService).hentDokument(eq("2"), anyString());
        verify(dokumentHentingService).hentDokument(eq("3"), anyString());

        verify(eessiService).opprettBucOgSed(any(), eq(BucType.LA_BUC_01), anyList(), anyList(), vedleggCaptor.capture());

        assertThat(vedleggCaptor.getValue()).extracting(Vedlegg::getTittel)
            .containsExactlyInAnyOrder("1", "1", "1", "2", "3");
    }

    @Test
    public void hentBucer() throws IOException, MelosysException {
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

    private BucInformasjon bucInformasjon() {
        return new BucInformasjon(
            defaultEasyRandom().nextObject(String.class),
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