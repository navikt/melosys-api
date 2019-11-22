package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingOversiktDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.HenleggelseDto;
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

import static no.nav.melosys.tjenester.gui.util.FagsakBehandlingFactory.fagsakMedBehandlinger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(FagsakTjenesteTest.class);
    private static final String FAGSAKER_SCHEMA = "fagsaker-schema.json";
    private static final String SOK_FAGSAKER_SCHEMA = "fagsaker-sok-schema.json";

    private static final String FNR = "12345678901";
    private static FagsakService fagsakService;

    private static TilgangService tilgangService;

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
    public void fagsakSchemaValidering() throws IOException, JSONException {
        FagsakDto fagsakDto = random.nextObject(FagsakDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(fagsakDto);
        valider(jsonString, FAGSAKER_SCHEMA, log);
    }

    @Test
    public void fagsakSøkSchemaValidering() throws IOException, JSONException {
        List<FagsakOppsummeringDto> fagsakOppsummeringDtoList = random.objects(FagsakOppsummeringDto.class, 1).collect(Collectors.toList());
        List<BehandlingOversiktDto> behandlingOversiktDtoer = random.objects(BehandlingOversiktDto.class, 1).collect(Collectors.toList());
        behandlingOversiktDtoer.get(0).setLand(Collections.singletonList(Landkoder.NO.getKode()));
        fagsakOppsummeringDtoList.get(0).setBehandlingOversikter(behandlingOversiktDtoer);

        validerArray(fagsakOppsummeringDtoList, SOK_FAGSAKER_SCHEMA, log);
    }

    @Test
    public final void hentFagsakGir200OkOgDto() throws Exception {
        Fagsak fagsak = lagFagsak();
        testHentFagsak("123", Response.status(Status.OK).entity(lagFagsakDto(fagsak)).build());
    }

    private void testHentFagsak(String saksnr, Response forventning) throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        Response resultat = instans.hentFagsak(saksnr);
        assertThat(resultat.getStatusInfo()).isEqualTo(forventning.getStatusInfo());
        if (forventning.getEntity() == null) {
            assertThat(resultat.getEntity()).isNull();
        } else {
            assertThat(resultat.getEntity()).usingRecursiveComparison().isEqualTo(forventning.getEntity());
        }
    }

    @Test
    public final void hentFagsakerGirIkkeTomListe() throws Exception {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(FNR);
        List<FagsakOppsummeringDto> forventet = Collections.singletonList(lagFagsakOppsummeringDto(behandling));
        assertThat(forventet.size()).isEqualTo(resultat.size());
        for (int i = 0; i < forventet.size(); i++) {
            assertThat(resultat.get(i)).usingRecursiveComparison().isEqualTo(forventet.get(i));
        }
    }

    @Test
    public final void hentFagsaker_utenFnr_girBadRequest() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(lagFagsak());
        Throwable unntak = catchThrowable(() -> instans.hentFagsaker(null));
        assertThat(unntak).isInstanceOf(BadRequestException.class);
    }

    @Test
    public final void opprettSak_utenFnr_badRequestException() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        OpprettSakDto dto = new OpprettSakDto();
        Throwable unntak = catchThrowable(() -> instans.opprettFagsak(dto));
        assertThat(unntak).isInstanceOf(BadRequestException.class);
    }

    @Test
    public final void opprettSak_sjekkerTilgangOgKallerService() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        OpprettSakDto dto = new OpprettSakDto();
        dto.brukerID = "brukerID";
        instans.opprettFagsak(dto);
        verify(tilgangService).sjekkFnr(eq(dto.brukerID));
        verify(fagsakService).bestillNySakOgBehandling(eq(dto));
    }

    @Test
    public final void henleggFagsakSenderSaksnummerFritekstOgBegrunnelseTilService() throws Exception {
        HenleggelseDto henleggelseDto = new HenleggelseDto();
        String begrunnelseKode = "GOD_GRUNN_TIL_HENLEGGELSE";
        String fritekst = "Dette er fritekst";
        henleggelseDto.setBegrunnelseKode(begrunnelseKode);
        henleggelseDto.setFritekst(fritekst);

        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        String saksnummer = "123";
        Response resultat = instans.henleggFagsak(saksnummer, henleggelseDto);

        assertThat(resultat.getStatusInfo()).isEqualTo(Status.OK);
        verify(fagsakService).henleggFagsak(saksnummer, begrunnelseKode, fritekst);
    }
    @Test
    public final void henleggFagsak_ingenSakFinnes_kasterIkkeFunnet() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(null);
        expectedException.expect(IkkeFunnetException.class);

        instans.henleggFagsak("Finnes ikke", new HenleggelseDto());

        verify(fagsakService, never()).henleggFagsak(anyString(), anyString(), anyString());
    }

    @Test
    public final void henleggFagsakKasterExceptionNårIkkeTilgangTilSak() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);

        doThrow(SikkerhetsbegrensningException.class).when(tilgangService).sjekkSak(fagsak);

        expectedException.expect(SikkerhetsbegrensningException.class);
        instans.henleggFagsak("123", new HenleggelseDto());

        verify(fagsakService, never()).henleggFagsak(anyString(), anyString(), anyString());
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
        List<FagsakOppsummeringDto> fagsakOppsummeringDtoer = instans.hentFagsaker(FNR);
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
    public final void avsluttSakSomBortfalt_sakEksisterer_kallerFagservice() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        String saksnummer = "123";
        Response resultat = instans.avsluttSakSomBortfalt(saksnummer);

        assertThat(resultat.getStatusInfo()).isEqualTo(Status.NO_CONTENT);
        verify(fagsakService).avsluttSakSomBortfalt(fagsak);
    }

    @Test
    public final void avsluttSakSomBortfalt_sakEksistererIkke_kasterException() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        doThrow(SikkerhetsbegrensningException.class).when(tilgangService).sjekkSak(fagsak);

        expectedException.expect(SikkerhetsbegrensningException.class);
        instans.avsluttSakSomBortfalt("123");

        verify(fagsakService, never()).henleggFagsak(anyString(), anyString(), anyString());
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) throws Exception {
        tilgangService = mock(TilgangService.class);
        fagsakService = mock(FagsakService.class);
        SaksopplysningerService saksopplysningerService = mock(SaksopplysningerService.class);
        SoeknadService søknadService = mock(SoeknadService.class);
        PersonDokument personDokument = (PersonDokument)FagsakBehandlingFactory.lagPersonSaksopplysning().getDokument();
        when(saksopplysningerService.finnPersonOpplysninger(eq(1L))).thenReturn(Optional.ofNullable(personDokument));
        SoeknadDokument søknadDokument = (SoeknadDokument) FagsakBehandlingFactory.lagSøknadOpplysning().getDokument();
        when(søknadService.finnSøknad(eq(1L))).thenReturn(Optional.ofNullable(søknadDokument));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        when(fagsakService.hentFagsak("Finnes ikke")).thenThrow(new IkkeFunnetException("Finnes ikke"));
        ArrayList<Fagsak> fagsaker = new ArrayList<>();
        fagsaker.add(fagsak);
        doReturn(fagsaker).when(fagsakService).hentFagsakerMedAktør(eq(Aktoersroller.BRUKER), eq(FNR));
        return new FagsakTjeneste(fagsakService, saksopplysningerService, søknadService, tilgangService);
    }

    private static FagsakOppsummeringDto lagFagsakOppsummeringDto(Behandling behandling) {
        FagsakOppsummeringDto result = new FagsakOppsummeringDto();
        result.setSammensattNavn("UKJENT");
        BehandlingOversiktDto behandlingOversiktDto = new BehandlingOversiktDto();
        behandlingOversiktDto.setBehandlingID(behandling.getId());
        result.setBehandlingOversikter(Collections.singletonList(behandlingOversiktDto));
        return result;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
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
}