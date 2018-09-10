package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.service.journalforing.JournalforingService;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.DokumentDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import no.nav.melosys.tjenester.gui.jackson.JacksonModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JournalforingTjenesteTest {

    private JournalforingTjeneste tjeneste;

    @Mock
    private JournalforingService journalføringService;

    @Mock
    private KodeverkService kodeverkService;

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new JacksonModule(kodeverkService));

        tjeneste = new JournalforingTjeneste(journalføringService);
    }

    @Test
    public void jfrJsonUt() {
        JournalpostDto dto = new JournalpostDto();
        dto.setBrukerID("12345");
        dto.setAvsenderID("56890");
        dto.setErBrukerAvsender(false);
        DokumentDto dokumentDto = new DokumentDto();
        dokumentDto.setID("DOK_ID");
        dokumentDto.setTittel(DokumentTittel.SØKNAD_MEDLEMSSKAP.getBeskrivelse());
        dokumentDto.setMottattDato(LocalDateTime.now());
        dto.setDokument(dokumentDto);

        try {
            String json = mapper.writeValueAsString(dto);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fagsakJsonUt() {
        FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
        fagsakOppsummeringDto.setSaksnummer("MEL-1234");
        fagsakOppsummeringDto.setSoknadsperiode(new PeriodeDto(LocalDate.now(), LocalDate.MAX));
        fagsakOppsummeringDto.setSakstype(FagsakType.EU_EØS);
        fagsakOppsummeringDto.setBehandlingstype(BehandlingType.SØKNAD);
        fagsakOppsummeringDto.setBehandlingsstatus(BehandlingStatus.UNDER_BEHANDLING);
        fagsakOppsummeringDto.setLand(Arrays.asList("DK","SE"));
        fagsakOppsummeringDto.setOpprettetDato(Instant.MIN);

        try {
            System.out.println(mapper.writeValueAsString(fagsakOppsummeringDto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void jfrJsonInn() {

        String json = "{\"journalpostID\":\"415782379\",\"oppgaveID\":\"5\",\"brukerID\":\"FJERNET\",\"avsenderID\":\"FJERNET\",\"dokumentID\":\"425226004\",\"dokumenttittel\":\"Søknad om medlemskap\",\"vedleggstitler\":[],\"fagsak\":{\"soknadsperiode\":{\"fom\":\"2018-02-01\",\"tom\":\"2018-12-03\"},\"land\":[\"HU\"]}}";

        try {
            mapper.readValue(json, JournalforingDto.class);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}