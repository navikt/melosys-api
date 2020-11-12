package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.HenleggFagsakService;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.service.sak.VideresendSoknadService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.util.FagsakBehandlingFactory;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static no.nav.melosys.tjenester.gui.util.FagsakBehandlingFactory.fagsakMedBehandlinger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(FagsakTjenesteTest.class);
    private static final String FAGSAKER_SCHEMA = "fagsaker-schema.json";
    private static final String FAGSAKER_OPPRETT_SCHEMA = "fagsaker-opprett-post-schema.json";
    private static final String SOK_FAGSAKER_SCHEMA = "fagsaker-sok-schema.json";
    private static final String SOK_FAGSAKER_POST_SCHEMA = "fagsaker-sok-post-schema.json";
    private static final String FAGSAKER_UTPEK_POST_SCHEMA = "fagsaker-utpek-post-schema.json";
    private static final String FAGSAKER_VIDERESEND_POST_SCHEMA = "fagsaker-henleggvideresend-post-schema.json";
    private static final String FAGSAKSER_HENLEGG_POST_SCHEMA = "fagsaker-henlegg-post-schema.json";

    private static final String FNR = "12345678901";
    private static FagsakService fagsakService;
    private static HenleggFagsakService henleggFagsakService;
    private static TilgangService tilgangService;
    private static UtpekingService utpekingService;
    private static VideresendSoknadService videresendSoknadService;

    private EasyRandom random;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .excludeField(named("tilleggsinformasjonDetaljer").and(ofType(TilleggsinformasjonDetaljer.class)).and(inClass(Tilleggsinformasjon.class)))
            .stringLengthRange(2, 10)
            .randomize(MidlertidigPostadresse.class, () -> Math.random() > 0.5 ? random.nextObject(MidlertidigPostadresseNorge.class) : random.nextObject(MidlertidigPostadresseUtland.class))
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9)));
    }

    @Test
    public void videresendSchemaValidering() throws IOException, JSONException {
        VideresendDto fagsakDto = random.nextObject(VideresendDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(fagsakDto);
        valider(jsonString, FAGSAKER_VIDERESEND_POST_SCHEMA, log);
    }

    @Test
    public void fagsakSchemaValidering() throws IOException, JSONException {
        FagsakDto fagsakDto = random.nextObject(FagsakDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(fagsakDto);
        valider(jsonString, FAGSAKER_SCHEMA, log);
    }

    @Test
    public void fagsakOpprettSchemaValidering() throws IOException, JSONException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);

        String jsonString = new ObjectMapper().registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).writeValueAsString(opprettSakDto);
        valider(jsonString, FAGSAKER_OPPRETT_SCHEMA, log);
    }

    @Test
    public void fagsakUtpekSchemaValidering() throws IOException {
        UtpekDto utpekDto = random.nextObject(UtpekDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(utpekDto);
        valider(jsonString, FAGSAKER_UTPEK_POST_SCHEMA, log);
    }

    @Test
    public void fagsakSøkSchemaValidering() throws Exception {
        valider(new FagsakSokDto("123", "MEL-123"), SOK_FAGSAKER_POST_SCHEMA);

        List<FagsakOppsummeringDto> fagsakOppsummeringDtoList = random.objects(FagsakOppsummeringDto.class, 1).collect(Collectors.toList());
        List<BehandlingOversiktDto> behandlingOversiktDtoer = random.objects(BehandlingOversiktDto.class, 1).collect(Collectors.toList());
        behandlingOversiktDtoer.get(0).setLand(Collections.singletonList(Landkoder.NO.getKode()));
        fagsakOppsummeringDtoList.get(0).setBehandlingOversikter(behandlingOversiktDtoer);

        validerArray(fagsakOppsummeringDtoList, SOK_FAGSAKER_SCHEMA, log);
    }

    @Test
    public final void hentFagsakGir200OkOgDto() throws Exception {
        Fagsak fagsak = lagFagsak();
        testHentFagsak("123", ResponseEntity.status(HttpStatus.OK).body(lagFagsakDto(fagsak)));
    }

    private void testHentFagsak(String saksnr, ResponseEntity forventning) throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        ResponseEntity resultat = instans.hentFagsak(saksnr);
        assertThat(resultat.getStatusCode()).isEqualTo(forventning.getStatusCode());
        if (forventning.getBody() == null) {
            assertThat(resultat.getBody()).isNull();
        } else {
            assertThat(resultat.getBody()).usingRecursiveComparison().isEqualTo(forventning.getBody());
        }
    }

    @Test
    public final void hentFagsaker_medFnr_verifiserErMappetKorrekt() throws Exception {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        FagsakSokDto søkDto = new FagsakSokDto(FNR, null);
        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(søkDto);
        List<FagsakOppsummeringDto> forventet = Collections.singletonList(lagFagsakOppsummeringDto(behandling));
        assertThat(forventet.size()).isEqualTo(resultat.size());
        for (int i = 0; i < forventet.size(); i++) {
            assertThat(resultat.get(i)).usingRecursiveComparison().isEqualTo(forventet.get(i));
        }
    }

    @Test
    public void hentFagsaker_medSaksnummer_finnerSakMottarListeMedEttElement() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        when(fagsakService.finnFagsakFraSaksnummer("123")).thenReturn(Optional.of(fagsak));

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(null, "123"));
        assertThat(resultat).hasSize(1).element(0).extracting(FagsakOppsummeringDto::getSaksnummer).isEqualTo(fagsak.getSaksnummer());
    }

    @Test
    public void hentFagsaker_medSaksnummer_finnerIkkeSakMottarTomListe() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(null, "NEI-123"));
        assertThat(resultat).isEmpty();
    }

    @Test
    public final void fagsakogbehandling_tilFagsakOppsummeringOgBehandlingOversiktDtoer() throws Exception {
        Fagsak fagsak = fagsakMedBehandlinger(Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVSLUTTET,
            Behandlingsstatus.AVSLUTTET);

        fagsak.setSaksnummer("MEL-13");
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        Instant reqDateInstant = Instant.parse("2019-01-01T10:15:30.00Z");
        Instant endretDateInstant = Instant.parse("2019-01-01T10:15:30.00Z");

        fagsak.setRegistrertDato(reqDateInstant);
        fagsak.setEndretDato(endretDateInstant);

        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        List<FagsakOppsummeringDto> fagsakOppsummeringDtoer = instans.hentFagsaker(new FagsakSokDto(FNR, null));
        assertThat(fagsakOppsummeringDtoer.size()).isEqualTo(1);
        FagsakOppsummeringDto fagsakOppsummeringDto = fagsakOppsummeringDtoer.get(0);
        assertThat(fagsakOppsummeringDto.getBehandlingOversikter().size()).isEqualTo(3);

        assertThat(fagsakOppsummeringDto.getSaksnummer()).isEqualTo("MEL-13");
        assertThat(fagsakOppsummeringDto.getOpprettetDato()).isEqualTo(reqDateInstant);
        assertThat(fagsakOppsummeringDto.getSammensattNavn()).isEqualTo("Joe Moe");

        BehandlingOversiktDto behandlingFørst = fagsakOppsummeringDto.getBehandlingOversikter().get(0);
        assertThat(behandlingFørst.getBehandlingID()).isEqualTo(1L);
        assertThat(behandlingFørst.getBehandlingsstatus().getKode()).isEqualTo("UNDER_BEHANDLING");
        assertThat(behandlingFørst.getBehandlingstype().getKode()).isEqualTo("SOEKNAD");
        assertThat(behandlingFørst.getOpprettetDato()).isEqualTo(Instant.parse("2019-01-10T10:37:30.00Z"));
        assertThat(behandlingFørst.getLand().get(0)).isEqualTo("DK");

        assertThat(behandlingFørst.getPeriode().getFom()).isEqualTo(LocalDate.of(2019,1,1));
        assertThat(behandlingFørst.getPeriode().getTom()).isEqualTo(LocalDate.of(2019,2,1));
    }

    @Test
    public final void opprettSak_utenFnr_badRequestException() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        OpprettSakDto dto = new OpprettSakDto();
        Throwable unntak = catchThrowable(() -> instans.opprettFagsak(dto));
        assertThat(unntak).isInstanceOf(FunksjonellException.class);
    }

    @Test
    public final void opprettSak_sjekkerTilgangOgKallerService() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        OpprettSakDto dto = new OpprettSakDto();
        dto.setBrukerID("brukerID");
        instans.opprettFagsak(dto);
        verify(tilgangService).sjekkFnr(eq(dto.getBrukerID()));
        verify(fagsakService).bestillNySakOgBehandling(eq(dto));
    }

    @Test
    public final void henleggFagsakSenderSaksnummerFritekstOgBegrunnelseTilService() throws Exception {
        HenleggelseDto henleggelseDto = new HenleggelseDto();
        String begrunnelseKode = Henleggelsesgrunner.OPPHOLD_UTL_AVLYST.getKode();
        String fritekst = "Dette er fritekst";
        henleggelseDto.setBegrunnelseKode(begrunnelseKode);
        henleggelseDto.setFritekst(fritekst);

        valider(henleggelseDto, FAGSAKSER_HENLEGG_POST_SCHEMA);

        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        String saksnummer = "123";
        ResponseEntity<Void> resultat = instans.henleggFagsak(saksnummer, henleggelseDto);

        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(henleggFagsakService).henleggFagsak(saksnummer, begrunnelseKode, fritekst);
    }

    @Test
    public final void avsluttSakSomBortfalt_sakEksisterer_kallerFagservice() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        String saksnummer = "123";
        ResponseEntity resultat = instans.avsluttSakSomBortfalt(saksnummer);

        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(fagsakService).avsluttSakSomBortfalt(fagsak);
    }

    @Test
    public final void avsluttSakSomBortfalt_sakEksistererIkke_kasterException() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        doThrow(SikkerhetsbegrensningException.class).when(tilgangService).sjekkSak(fagsak);

        expectedException.expect(SikkerhetsbegrensningException.class);
        instans.avsluttSakSomBortfalt("123");

        verify(henleggFagsakService, never()).henleggFagsak(anyString(), anyString(), anyString());
    }

    @Test
    public final void avsluttSakManuelt_sakEksisterer_avsluttes() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        instans.avsluttSakManuelt("123");
        verify(fagsakService).avsluttFagsakOgBehandlingValiderBehandlingstype(eq(fagsak), eq(fagsak.hentAktivBehandling()));
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) throws Exception {
        tilgangService = mock(TilgangService.class);
        fagsakService = mock(FagsakService.class);
        henleggFagsakService = mock(HenleggFagsakService.class);
        utpekingService = mock(UtpekingService.class);
        videresendSoknadService = mock(VideresendSoknadService.class);
        SaksopplysningerService saksopplysningerService = mock(SaksopplysningerService.class);
        BehandlingsgrunnlagService behandlingsgrunnlagService = mock(BehandlingsgrunnlagService.class);
        PersonDokument personDokument = (PersonDokument)FagsakBehandlingFactory.lagPersonSaksopplysning().getDokument();
        when(saksopplysningerService.finnPersonOpplysninger(eq(1L))).thenReturn(Optional.ofNullable(personDokument));
        Soeknad søknadDokument = FagsakBehandlingFactory.lagSøknadDokument();
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknadDokument);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(eq(1L))).thenReturn(Optional.of(behandlingsgrunnlag));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        ArrayList<Fagsak> fagsaker = new ArrayList<>();
        fagsaker.add(fagsak);
        doReturn(fagsaker).when(fagsakService).hentFagsakerMedAktør(eq(Aktoersroller.BRUKER), eq(FNR));
        return new FagsakTjeneste(fagsakService, henleggFagsakService, saksopplysningerService, behandlingsgrunnlagService, tilgangService, utpekingService, videresendSoknadService);
    }

    private static FagsakOppsummeringDto lagFagsakOppsummeringDto(Behandling behandling) {
        FagsakOppsummeringDto result = new FagsakOppsummeringDto();
        result.setSaksnummer("MEL-1");
        result.setSammensattNavn("UKJENT");
        BehandlingOversiktDto behandlingOversiktDto = new BehandlingOversiktDto();
        behandlingOversiktDto.setBehandlingID(behandling.getId());
        result.setBehandlingOversikter(Collections.singletonList(behandlingOversiktDto));
        return result;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");
        fagsak.setBehandlinger(Collections.emptyList());
        return fagsak;
    }

    private static FagsakDto lagFagsakDto(Fagsak fagsak) {
        FagsakDto resultat = new FagsakDto();
        resultat.setEndretDato(fagsak.getEndretDato());
        resultat.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        resultat.setRegistrertDato(fagsak.getRegistrertDato());
        resultat.setSaksnummer(fagsak.getSaksnummer());
        resultat.setSakstype(fagsak.getType());
        resultat.setSaksstatus(fagsak.getStatus());
        return resultat;
    }

    @Test
    public void utpekLovvalgsland() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste fagsakTjeneste = lagFagsakTjeneste(fagsak);
        UtpekDto utpekDto = random.nextObject(UtpekDto.class);

        when(fagsakService.hentFagsak(any())).thenReturn(fagsak);

        fagsakTjeneste.utpekLovvalgsland(fagsak.getSaksnummer(), utpekDto);

        verify(tilgangService).sjekkSak(fagsak);
        verify(utpekingService).utpekLovvalgsland(eq(fagsak), eq(utpekDto.getMottakerinstitusjoner()), eq(utpekDto.getFritekstSed()), eq(utpekDto.getFritekstBrev()));
    }
}