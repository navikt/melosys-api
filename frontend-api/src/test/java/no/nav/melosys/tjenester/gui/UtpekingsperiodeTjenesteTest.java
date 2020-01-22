package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtpekingsperiodeTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(UtpekingsperiodeTjenesteTest.class);
    private static final String UTPEKINGSPERIODER_SCHEMA = "utpekingsperioder-schema.json";
    private EasyRandom random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(2, 4));

    @Test
    public void postUtpekingsperioder() throws IOException {
        // FIXME - samme skjema for GET og POST - test endepunktene med validert JSON
        UtpekingsperioderDto utpekingsperioderDto = random.nextObject(UtpekingsperioderDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(utpekingsperioderDto);
        valider(jsonString, UTPEKINGSPERIODER_SCHEMA, log);
    }
}
