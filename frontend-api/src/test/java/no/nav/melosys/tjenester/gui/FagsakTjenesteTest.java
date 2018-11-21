package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(FagsakTjenesteTest.class);

    private static final String FAGSAKER_SCHEMA = "fagsaker-schema.json";
    private static final String SOK_FAGSAKER_SCHEMA = "sok-fagsaker-schema.json";

    private static final String FNR = "12345678901";

    private String schemaType;

    private EnhancedRandom random;

    @Before
    public void setUp() {

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .exclude(FieldDefinitionBuilder.field().named("tilleggsinformasjonDetaljer").ofType(TilleggsinformasjonDetaljer.class).inClass(Tilleggsinformasjon.class).get())
            .stringLengthRange(2, 10)
            .randomize(MidlertidigPostadresse.class, (Randomizer<MidlertidigPostadresse>) () -> Math.random() > 0.5 ? random.nextObject(MidlertidigPostadresseNorge.class) : random.nextObject(MidlertidigPostadresseUtland.class))
            .build();
    }

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Test
    public void fagsakSchemaValidering() throws IOException, JSONException {
        FagsakDto fagsakDto = random.nextObject(FagsakDto.class);

        for (BehandlingDto b : fagsakDto.getBehandlinger()) {
            // Gyldige adresser
            for (OrganisasjonDokument org : b.getSaksopplysninger().getOrganisasjoner()) {
                SemistrukturertAdresse adresse = random.nextObject(SemistrukturertAdresse.class);
                adresse.setGyldighetsperiode(new Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));
                org.getOrganisasjonDetaljer().forretningsadresse = new ArrayList<>();
                org.getOrganisasjonDetaljer().forretningsadresse.add(adresse);
                org.getOrganisasjonDetaljer().postadresse = new ArrayList<>();
                org.getOrganisasjonDetaljer().postadresse.add(adresse);
            }

        }

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(fagsakDto);
        schemaType = FAGSAKER_SCHEMA;
        valider(jsonString, log);
    }

    @Test
    public void fagsakSøkSchemaValidering() throws IOException, JSONException {
        List<FagsakOppsummeringDto> fagsakOppsummeringDtoList = random.randomListOf(2, FagsakOppsummeringDto.class);
        fagsakOppsummeringDtoList.forEach(fagsakOppsummeringDto -> fagsakOppsummeringDto.setSoknadsperiode(new PeriodeDto(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))));

        schemaType = SOK_FAGSAKER_SCHEMA;
        validerListe(fagsakOppsummeringDtoList, log);
    }

    @Test
    public final void hentFagsakGir200OkOgDto() throws Exception {
        Fagsak fagsak = lagFagsak();
        testHentFagsak("123", Response.status(Status.OK).entity(lagFagsakDto(fagsak)).build());
    }

    @Test
    public final void hentIngenFagsakGir404() throws Exception {
        testHentFagsak(String.valueOf(Long.MAX_VALUE), Response.status(Status.NOT_FOUND).build());
    }

    private void testHentFagsak(String saksnr, Response forventning) throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        Response resultat = instans.hentFagsak(saksnr);
        assertThat(resultat.getStatusInfo()).isEqualTo(forventning.getStatusInfo());
        if (forventning.getEntity() == null) {
            assertThat(resultat.getEntity()).isNull();
        } else {
            assertThat(resultat.getEntity()).isEqualToComparingFieldByFieldRecursively(forventning.getEntity());
        }
    }

    @Test
    public final void hentFagsakerGirIkkeTomListe() throws Exception {
        Fagsak fagsak = lagFagsak();
        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        List<FagsakOppsummeringDto> resultat = instans.hentFagsaker(FNR);
        List<FagsakOppsummeringDto> forventet = Collections.singletonList(lagFagsakOppsummeringDto(fagsak));
        assertThat(forventet.size()).isEqualTo(resultat.size());
        for (int i = 0; i < forventet.size(); i++) {
            assertThat(forventet.get(i)).isEqualToComparingFieldByFieldRecursively(resultat.get(i));
        }
    }

    @Test
    public final void hentFagsakerUtenFnrGirBadRequestException() throws Exception {
        FagsakTjeneste instans = lagFagsakTjeneste(lagFagsak());
        Throwable unntak = catchThrowable(() -> instans.hentFagsaker(null));
        assertThat(unntak).isInstanceOf(BadRequestException.class);
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) throws Exception {
        Tilgang tilgang = mock(Tilgang.class);
        FagsakService fagsakService = mock(FagsakService.class);
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        when(fagsakService.hentFagsakerMedAktør(eq(RolleType.BRUKER), eq(FNR)))
            .thenReturn(Collections.singletonList(fagsak));
        FagsakTjeneste instans = new FagsakTjeneste(fagsakService, tilgang);
        return instans;
    }

    private static FagsakOppsummeringDto lagFagsakOppsummeringDto(Fagsak fagsak) {
        FagsakOppsummeringDto result = new FagsakOppsummeringDto();
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
        resultat.setStatus(fagsak.getStatus());
        return resultat;
    }

}