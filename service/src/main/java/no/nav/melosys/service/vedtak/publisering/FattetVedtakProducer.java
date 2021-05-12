package no.nav.melosys.service.vedtak.publisering;


import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.service.JsonSchemaValidator;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FattetVedtakProducer {
    private static final Logger log = LoggerFactory.getLogger(FattetVedtakProducer.class);
    private static final String FATTET_VEDTAK_SCHEMA = "fattet-vedtak-schema.json";

    private final ObjectMapper objectMapper;

    @Autowired
    public FattetVedtakProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publiserMelding(FattetVedtak fattetVedtak) {
        validerMelding(fattetVedtak);
        //TODO Implementer med Aiven oppsett
    }

    private void validerMelding(FattetVedtak fattetVedtak) {
        new JsonSchemaValidator(objectMapper).valider(fattetVedtak, FATTET_VEDTAK_SCHEMA);
    }
}
