package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.VedleggTittel;
import no.nav.melosys.service.journalforing.JournalforingService;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.DokumentDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
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

    @Before
    public void setUp() {
        tjeneste = new JournalforingTjeneste(journalføringService);
    }

    @Test
    public void jsonUt() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());

        JournalpostDto dto = new JournalpostDto();
        dto.setJournalpostID("JOUR_321");
        dto.setBruker(new PersonDto("12345", "Bruker ABC"));
        dto.setAvsender(new PersonDto("56890", "Avsender XYZ"));
        dto.setErBrukerAvsender(false);
        DokumentDto dokumentDto = new DokumentDto();
        dokumentDto.setNavn("Navn");
        dokumentDto.setMottattDato(LocalDate.now());
        dokumentDto.setSakstype(FagsakType.EU_EØS);
        dokumentDto.setTittel(DokumentTittel.SØKNAD_MEDLEMSSKAP);
        List<VedleggTittel> titler = new ArrayList<>();
        titler.add(VedleggTittel.TODO_1);
        titler.add(VedleggTittel.TODO_2);
        dokumentDto.setVedleggstitler(titler);
        dto.setDokument(dokumentDto);

        try {
            String json = mapper.writeValueAsString(dto);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}