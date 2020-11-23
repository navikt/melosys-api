package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PersonTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(PersonTjenesteTest.class);

    @Mock
    private PersonTjeneste personTjeneste;

    @Before
    public void setUp() throws Exception {
        PersonDto person = new EasyRandom(defaultEasyRandomParameters()
            .randomize(named("fnrAnnenForelder").and(ofType(String.class)), new NumericStringRandomizer(11)))
            .nextObject(PersonDto.class);
        when(personTjeneste.getPerson(anyString())).thenReturn(ResponseEntity.ok(person));
    }

    @Test
    public void personSchemaValidering() throws Exception {
        ResponseEntity person = personTjeneste.getPerson("12345678910");
        ObjectMapper mapper = objectMapperMedKodeverkServiceStub();
        String jsonInString = mapper
            .writerWithView(DokumentView.FrontendApi.class)
            .writeValueAsString(person.getBody());
        valider(jsonInString, "personer-schema.json", log);
    }
}
