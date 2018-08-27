package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersonSerializerTest {

    private DokumentFactory factory;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);

        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("MOCK");

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new SimpleModule().addSerializer(new FellesKodeverkSerializer(kodeverkService)));
        objectMapper.registerModule(new SimpleModule().addSerializer(new PersonSerializer()));
        objectMapper.registerModule(new SimpleModule().addSerializer(new PersonhistorikkSerializer()));
    }

    @Test
    public void serialiserPersonMedHistorikk() throws Exception {

        PersonDokument personDokument = (PersonDokument) lagDokument("88888888882.xml", SaksopplysningType.PERSONOPPLYSNING, "3.0");
        PersonhistorikkDokument personhistorikkDokument = (PersonhistorikkDokument) lagDokument("88888888882_historikk.xml", SaksopplysningType.PERSONHISTORIKK, "3.4");

        assertThat(personDokument).isNotNull();
        assertThat(personhistorikkDokument).isNotNull();

        SaksopplysningerDto saksopplysningerDto = new SaksopplysningerDto();
        saksopplysningerDto.setPerson(personDokument);
        saksopplysningerDto.setPersonhistorikk(personhistorikkDokument);

        String json = objectMapper.writeValueAsString(saksopplysningerDto);

        assertThat(json).isNotNull();
    }

    private SaksopplysningDokument lagDokument(String ressurs, SaksopplysningType type, String versjon) {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, Charset.forName("UTF-8")))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(type);
            saksopplysning.setVersjon(versjon);

            factory.lagDokument(saksopplysning);

            return saksopplysning.getDokument();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
