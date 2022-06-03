package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.sak.*;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory.fagsakMedBehandlinger;
import static org.assertj.core.api.Assertions.*;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

class FagsakTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(FagsakTjenesteTest.class);
    private static final String FAGSAKER_SCHEMA = "fagsaker-schema.json";
    private static final String FAGSAKER_OPPRETT_SCHEMA = "fagsaker-opprett-post-schema.json";
    private static final String SOK_FAGSAKER_SCHEMA = "fagsaker-sok-schema.json";
    private static final String SOK_FAGSAKER_POST_SCHEMA = "fagsaker-sok-post-schema.json";
    private static final String FAGSAKER_UTPEK_POST_SCHEMA = "fagsaker-utpek-post-schema.json";
    private static final String FAGSAKER_VIDERESEND_POST_SCHEMA = "fagsaker-henleggvideresend-post-schema.json";
    private static final String FAGSAKSER_HENLEGG_POST_SCHEMA = "fagsaker-henlegg-post-schema.json";

    private static final String FNR = "12345678901";
    private static final String ORGNR = "111111111";

    private static FagsakService fagsakService;
    private static OpprettNySakFraOppgave opprettNySakFraOppgave;
    private static Aksesskontroll aksesskontroll;
    private static HenleggFagsakService henleggFagsakService;
    private static UtpekingService utpekingService;
    private static OrganisasjonOppslagService organisasjonOppslagService;

    private EasyRandom random;

    @BeforeEach
    void setUp() {
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
    void videresendSchemaValidering() throws JsonProcessingException {
        VideresendDto videresendDto = new VideresendDto();
        videresendDto.setMottakerinstitusjon("SE:123");
        videresendDto.setFritekst("fri som fuglen");
        videresendDto.setVedlegg(Set.of(new VedleggDto("1", "1")));

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(videresendDto);
        assertThatCode(() -> valider(jsonString, FAGSAKER_VIDERESEND_POST_SCHEMA, log)).doesNotThrowAnyException();
    }

    @Test
    void fagsakSchemaValidering() throws JsonProcessingException {
        FagsakDto fagsakDto = random.nextObject(FagsakDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(fagsakDto);
        assertThatCode(() -> valider(jsonString, FAGSAKER_SCHEMA, log)).doesNotThrowAnyException();
    }

    @Test
    void fagsakOpprettSchemaValidering() throws JsonProcessingException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);

        String jsonString = new ObjectMapper().registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).writeValueAsString(opprettSakDto);
        assertThatCode(() -> valider(jsonString, FAGSAKER_OPPRETT_SCHEMA, log)).doesNotThrowAnyException();
    }

    @Test
    void fagsakUtpekSchemaValidering() throws JsonProcessingException {
        UtpekDto utpekDto = new UtpekDto(Set.of("SE:123"), "fri SED", "fri brev");

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(utpekDto);
        assertThatCode(() -> valider(jsonString, FAGSAKER_UTPEK_POST_SCHEMA, log)).doesNotThrowAnyException();
    }

    @Test
    void fagsakSøkSchemaValidering() throws IOException {
        valider(new FagsakSokDto("123", "MEL-123", "111111111"), SOK_FAGSAKER_POST_SCHEMA);

        List<FagsakOppsummeringDto> fagsakOppsummeringDtoList = random.objects(FagsakOppsummeringDto.class, 1).collect(Collectors.toList());
        List<BehandlingOversiktDto> behandlingOversiktDtoer = random.objects(BehandlingOversiktDto.class, 1).collect(Collectors.toList());
        behandlingOversiktDtoer.get(0).setLand(new SoeknadslandDto(Collections.singletonList(Landkoder.NO.getKode()), false));
        fagsakOppsummeringDtoList.get(0).setBehandlingOversikter(behandlingOversiktDtoer);

        assertThatCode(() -> validerArray(fagsakOppsummeringDtoList, SOK_FAGSAKER_SCHEMA, log)).doesNotThrowAnyException();
    }

    @Test
    void hentFagsakGir200OkOgDto() {
        Fagsak fagsak = lagFagsak();
        testHentFagsak(ResponseEntity.status(HttpStatus.OK).body(lagFagsakDto(fagsak)));
    }

    private void testHentFagsak(ResponseEntity<FagsakDto> forventning) {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        ResponseEntity<FagsakDto> resultat = instans.hentFagsak("123");
        assertThat(resultat.getStatusCode()).isEqualTo(forventning.getStatusCode());
        if (forventning.getBody() == null) {
            assertThat(resultat.getBody()).isNull();
        } else {
            assertThat(resultat.getBody()).usingRecursiveComparison().isEqualTo(forventning.getBody());
        }
    }

    @Test
    void hentFagsaker_medFnr_verifiserErMappetKorrekt() {
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        FagsakSokDto søkDto = new FagsakSokDto(FNR, null, null);
        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(søkDto);
        List<FagsakOppsummeringDto> forventet = Collections.singletonList(lagFagsakOppsummeringDto(behandling));
        assertThat(forventet).hasSameSizeAs(resultat);
        for (int i = 0; i < forventet.size(); i++) {
            assertThat(resultat.get(i)).usingRecursiveComparison().isEqualTo(forventet.get(i));
        }
    }

    @Test
    void hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(aktoer));
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(FNR, null, null));
        assertThat(resultat).hasSize(1)
            .flatExtracting(FagsakOppsummeringDto::getNavn, FagsakOppsummeringDto::getHovedpartRolle)
            .containsExactly("UKJENT", Aktoersroller.BRUKER);
    }

    @Test
    void hentFagsaker_medOrgnr_verifiserErMappetKorrekt() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        Aktoer aktoer = new Aktoer();
        aktoer.setOrgnr(ORGNR);
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.setAktører(Set.of(aktoer));
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        var organisajonsdokument = new OrganisasjonDokument();
        organisajonsdokument.setNavn("Moe Organisasjon");
        when(organisasjonOppslagService.hentOrganisasjon(ORGNR)).thenReturn(organisajonsdokument);

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(null, null, ORGNR));
        assertThat(resultat).hasSize(1)
            .flatExtracting(FagsakOppsummeringDto::getNavn, FagsakOppsummeringDto::getHovedpartRolle)
            .containsExactly("Moe Organisasjon", Aktoersroller.VIRKSOMHET);
    }

    @Test
    void hentFagsaker_medTomtOrgnr_verifiserAtNavnErUkjent() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.setAktører(Set.of(aktoer));
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(null, null, ORGNR));
        assertThat(resultat).hasSize(1)
            .flatExtracting(FagsakOppsummeringDto::getNavn, FagsakOppsummeringDto::getHovedpartRolle)
            .containsExactly("UKJENT", Aktoersroller.VIRKSOMHET);
    }

    @Test
    void hentFagsaker_medSaksnummer_finnerSakMottarListeMedEttElement() {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        when(fagsakService.finnFagsakFraSaksnummer("123")).thenReturn(Optional.of(fagsak));

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(null, "123", null));
        assertThat(resultat).hasSize(1).element(0).extracting(FagsakOppsummeringDto::getSaksnummer).isEqualTo(fagsak.getSaksnummer());
    }

    @Test
    void hentFagsaker_medSaksnummer_finnerIkkeSakMottarTomListe() {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(new FagsakSokDto(null, "NEI-123", null));
        assertThat(resultat).isEmpty();
    }

    @Test
    void fagsakogbehandling_tilFagsakOppsummeringOgBehandlingOversiktDtoer() {
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
        List<FagsakOppsummeringDto> fagsakOppsummeringDtoer = instans.hentFagsaker(new FagsakSokDto(FNR, null, null));
        assertThat(fagsakOppsummeringDtoer).hasSize(1);
        FagsakOppsummeringDto fagsakOppsummeringDto = fagsakOppsummeringDtoer.get(0);
        assertThat(fagsakOppsummeringDto.getBehandlingOversikter()).hasSize(3);

        assertThat(fagsakOppsummeringDto.getSaksnummer()).isEqualTo("MEL-13");
        assertThat(fagsakOppsummeringDto.getOpprettetDato()).isEqualTo(reqDateInstant);
        assertThat(fagsakOppsummeringDto.getNavn()).isEqualTo("Joe Moe");

        BehandlingOversiktDto behandlingFørst = fagsakOppsummeringDto.getBehandlingOversikter().get(0);
        assertThat(behandlingFørst.getBehandlingID()).isEqualTo(1L);
        assertThat(behandlingFørst.getBehandlingsstatus().getKode()).isEqualTo("UNDER_BEHANDLING");
        assertThat(behandlingFørst.getBehandlingstype().getKode()).isEqualTo("SOEKNAD");
        assertThat(behandlingFørst.getOpprettetDato()).isEqualTo(Instant.parse("2019-01-10T10:37:30.00Z"));
        assertThat(behandlingFørst.getLand().landkoder.get(0)).isEqualTo("DK");
        assertThat(behandlingFørst.getLand().erUkjenteEllerAlleEosLand).isFalse();

        assertThat(behandlingFørst.getPeriode().getFom()).isEqualTo(LocalDate.of(2019, 1, 1));
        assertThat(behandlingFørst.getPeriode().getTom()).isEqualTo(LocalDate.of(2019, 2, 1));
    }

    @Test
    void opprettSak_utenFnr_badRequestException() {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        OpprettSakDto dto = new OpprettSakDto();
        Throwable unntak = catchThrowable(() -> instans.opprettFagsak(dto));
        assertThat(unntak).isInstanceOf(FunksjonellException.class);
    }

    @Test
    void opprettSak_sjekkerTilgangOgKallerService() {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        OpprettSakDto dto = new OpprettSakDto();
        dto.setBrukerID("brukerID");
        instans.opprettFagsak(dto);
        verify(aksesskontroll).autoriserFolkeregisterIdent(dto.getBrukerID());
        verify(opprettNySakFraOppgave).bestillNySakOgBehandling(dto);
    }

    @Test
    void henleggFagsakSenderSaksnummerFritekstOgBegrunnelseTilService() throws IOException {
        String begrunnelseKode = Henleggelsesgrunner.OPPHOLD_UTL_AVLYST.getKode();
        String fritekst = "Dette er fritekst";
        HenleggelseDto henleggelseDto = new HenleggelseDto(fritekst, begrunnelseKode);

        valider(henleggelseDto, FAGSAKSER_HENLEGG_POST_SCHEMA);

        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        String saksnummer = "123";
        ResponseEntity<Void> resultat = instans.henleggFagsak(saksnummer, henleggelseDto);

        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(henleggFagsakService).henleggFagsakEllerBehandling(saksnummer, begrunnelseKode, fritekst);
    }

    @Test
    void henleggSakSomBortfalt_sakEksisterer_kallerFagservice() {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        String saksnummer = "123";
        ResponseEntity<Void> resultat = instans.henleggSakSomBortfalt(saksnummer);
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);

        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(henleggFagsakService).henleggSakEllerBehandlingSomBortfalt("123");
    }

    @Test
    void avsluttSakManuelt_sakEksisterer_avsluttes() {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        instans.avsluttSakManuelt("123");
        verify(fagsakService).avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, fagsak.hentAktivBehandling());
    }

    @Test
    void videresendSøknad_utenVedlegg_feiler() {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        var videresendDto = new VideresendDto();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> instans.videresend("123", videresendDto))
            .withMessageContaining("uten vedlegg");
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) {
        aksesskontroll = mock(Aksesskontroll.class);
        fagsakService = mock(FagsakService.class);
        opprettNySakFraOppgave = mock(OpprettNySakFraOppgave.class);
        henleggFagsakService = mock(HenleggFagsakService.class);
        PersondataFasade persondataFasade = mock(PersondataFasade.class);
        organisasjonOppslagService = mock(OrganisasjonOppslagService.class);
        utpekingService = mock(UtpekingService.class);
        VideresendSoknadService videresendSoknadService = mock(VideresendSoknadService.class);
        SaksopplysningerService saksopplysningerService = mock(SaksopplysningerService.class);
        BehandlingsgrunnlagService behandlingsgrunnlagService = mock(BehandlingsgrunnlagService.class);
        Soeknad søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument();
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknadDokument);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(1L)).thenReturn(Optional.of(behandlingsgrunnlag));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        when(persondataFasade.hentSammensattNavn(any())).thenReturn("Joe Moe");
        if (fagsak != null) {
            doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedAktør(Aktoersroller.BRUKER, FNR);
            doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR);
        }
        return new FagsakTjeneste(fagsakService, aksesskontroll, behandlingsgrunnlagService, henleggFagsakService, opprettNySakFraOppgave,
            persondataFasade, saksopplysningerService, utpekingService, videresendSoknadService, organisasjonOppslagService);
    }

    private static FagsakOppsummeringDto lagFagsakOppsummeringDto(Behandling behandling) {
        FagsakOppsummeringDto result = new FagsakOppsummeringDto();
        result.setSakstype(Sakstyper.EU_EOS);
        result.setSaksstatus(Saksstatuser.OPPRETTET);
        result.setSaksnummer("MEL-1");
        result.setNavn("Joe Moe");
        result.setHovedpartRolle(Aktoersroller.BRUKER);

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
    void utpekLovvalgsland() {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste fagsakTjeneste = lagFagsakTjeneste(fagsak);
        UtpekDto utpekDto = new UtpekDto(Set.of("SE:123"), "Fri SED", "Fri brev");

        when(fagsakService.hentFagsak(any())).thenReturn(fagsak);

        fagsakTjeneste.utpekLovvalgsland(fagsak.getSaksnummer(), utpekDto);

        verify(aksesskontroll).autoriserSakstilgang(fagsak);
        verify(utpekingService).utpekLovvalgsland(fagsak, utpekDto.mottakerinstitusjoner(), utpekDto.fritekstSed(), utpekDto.fritekstBrev());
    }
}
