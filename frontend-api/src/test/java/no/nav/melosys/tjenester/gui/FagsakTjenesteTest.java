package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteTest extends JsonSchemaTest {

    private static final Logger logger = LoggerFactory.getLogger(FagsakTjenesteTest.class);

    private EnhancedRandom random;

    private FagsakTjeneste tjeneste;

    @Mock
    private FagsakService fagsakService;

    @Before
    public void setUp() {
        tjeneste = new FagsakTjeneste(fagsakService);

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
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
    public void hentFagsak() throws IOException {
        Fagsak fagsak = random.nextObject(Fagsak.class);
        when(fagsakService.hentFagsak(any())).thenReturn(fagsak);

        Response response = tjeneste.hentFagsak("TEST");
        FagsakDto fagsakDto = (FagsakDto) response.getEntity();
        // Saksopplysninger må genereres separat
        for (BehandlingDto b : fagsakDto.getBehandlinger()) {
            SaksopplysningerDto saksopplysninger = b.getSaksopplysninger();
            saksopplysninger.setArbeidsforhold(random.nextObject(ArbeidsforholdDokument.class));
            saksopplysninger.setInntekt(random.nextObject(InntektDokument.class, "tilleggsinformasjonDetaljer"));
            saksopplysninger.setMedlemskap(random.nextObject(MedlemskapDokument.class));
            List<OrganisasjonDokument> organisasjoner = new ArrayList<>();
            for (int i = 0; i < (random.nextInt(3) + 1); i++) {
                OrganisasjonDokument organisasjonDokument = random.nextObject(OrganisasjonDokument.class);
                // Gyldige adresser
                SemistrukturertAdresse adresse = random.nextObject(SemistrukturertAdresse.class);
                adresse.setGyldighetsperiode(new Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));
                organisasjonDokument.getOrganisasjonDetaljer().getForretningsadresse().add(adresse);
                organisasjonDokument.getOrganisasjonDetaljer().getPostadresse().add(adresse);
                organisasjoner.add(organisasjonDokument);
            }
            saksopplysninger.setOrganisasjoner(organisasjoner);
            saksopplysninger.setPerson(random.nextObject(PersonDokument.class));
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