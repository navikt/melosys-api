package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
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

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(FagsakTjenesteTest.class);

    private static final String FAGSAKER_SCHEMA = "fagsaker-schema.json";
    private static final String SOK_FAGSAKER_SCHEMA = "sok-fagsaker-schema.json";

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
            .randomize(MidlertidigPostadresse.class, (Randomizer<MidlertidigPostadresse>) () -> Math.random() > 0.5 ? EnhancedRandom.random(MidlertidigPostadresseNorge.class) : EnhancedRandom.random(MidlertidigPostadresseUtland.class))
            .stringLengthRange(2, 10)
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
}