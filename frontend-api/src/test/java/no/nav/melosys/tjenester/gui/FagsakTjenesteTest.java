package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteTest extends JsonSchemaTest {

    private static final Logger logger = LoggerFactory.getLogger(FagsakTjenesteTest.class);

    private EnhancedRandom random;

    @Before
    public void setUp() {

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .exclude(FieldDefinitionBuilder.field().named("tilleggsinformasjonDetaljer").ofType(TilleggsinformasjonDetaljer.class).inClass(Tilleggsinformasjon.class).get())
            .build();
    }

    @Override
    public String schemaNavn() {
        return "fagsaker-schema.json";
    }

    @Test
    public void fagsakSchemaValidering() throws IOException {
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

        try {
            hentSchema().validate(new JSONObject(jsonString));
        } catch (ValidationException e) {
            e.getCausingExceptions().stream()
                .map(ValidationException::toJSON)
                .forEach(jsonObject -> {
                    logger.error(jsonObject.toString());
                    System.out.println("----------------------------");
                });
            throw e;
        }
    }
}