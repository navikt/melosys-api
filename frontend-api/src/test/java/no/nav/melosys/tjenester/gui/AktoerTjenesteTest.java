package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AktoerTjeneste.class})
class AktoerTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(AktoerTjenesteTest.class);
    private static final String AKTOER_SCHEMA = "fagsaker-aktoerer-schema.json";
    private static final String AKTOER_POST_SCHEMA = "fagsaker-aktoerer-post-schema.json";

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private AktoerService aktoerService;
    @MockBean
    private FagsakService fagsakService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aktoerSchemaValidering() throws Exception {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1234");
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setRepresentererKode("BRUKER");
        aktoerDto.setOrgnr("123456789");
        aktoerDto.setDatabaseID(2L);
        aktoerDto.setPersonIdent("30056928150");


        validerArray(Collections.singletonList(aktoerDto), AKTOER_SCHEMA, log);
        valider(aktoerDto, AKTOER_POST_SCHEMA, log);
    }

    @Test
    void lagOppdaterAktoer() throws Exception {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1235");
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setRepresentererKode("BRUKER");
        aktoerDto.setOrgnr("123456789");
        aktoerDto.setDatabaseID(1L);

        Fagsak fagsak = lagFagsak();


        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(fagsak);
        when(aktoerService.lagEllerOppdaterAktoer(any(Fagsak.class), any(AktoerDto.class))).thenReturn(1L);


        mockMvc.perform(post("/api/fagsaker/{saksnummer}/aktoerer", "MELTEST-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aktoerDto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(aktoerDto, AktoerDto.class));
    }

    @Test
    void hentAktoer_tilAktoerDto() throws Exception {
        Aktoer aktoerMyndighet = new Aktoer();
        aktoerMyndighet.setId(39L);
        aktoerMyndighet.setAktørId("1235");
        aktoerMyndighet.setInstitusjonId("INST2");
        aktoerMyndighet.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktoerMyndighet.setOrgnr("100");
        aktoerMyndighet.setFagsak(lagFagsak());
        aktoerMyndighet.setUtenlandskPersonId("UTL");


        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(lagFagsak());
        when(aktoerService.hentfagsakAktører(any(), any(), any())).thenReturn(Collections.singletonList(aktoerMyndighet));


        mockMvc.perform(get("/api/fagsaker/{saksnummer}/aktoerer", "MELTEST-1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(Collections.singletonList(AktoerDto.tilDto(aktoerMyndighet)), new TypeReference<List<AktoerDto>>() {
            }));
    }

    @Test
    void slettAktoer_ok() throws Exception{
        mockMvc.perform(delete("/api/fagsaker/aktoerer/{databaseID}", 123L))
            .andExpect(status().isNoContent());
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }
}
