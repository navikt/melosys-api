package no.nav.melosys.domain.eessi.sed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class SedGrunnlagDtoTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serialiserSedDataDto() throws JsonProcessingException {
        SedDataDto sedDataDto = new SedDataDto();
        objectMapper.writeValueAsString(sedDataDto);
    }
}
