package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;
import static no.nav.melosys.domain.dokument.felles.Land.NORGE;
import static no.nav.melosys.domain.dokument.felles.Land.STORBRITANNIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MidlertidigPostadresseSerializerTest {

    private ObjectMapper mapper;
    @Mock
    private KodeverkService kodeverkService;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new MidlertidigPostadresseSerializer(kodeverkService));
        mapper.registerModule(simpleModule);
    }

    @Test
    void midlertidigPostadresseNorge() throws Exception {
        when(kodeverkService.dekod(POSTNUMMER, "0557", LocalDate.now())).thenReturn("Oslo");

        MidlertidigPostadresseNorge midlertidigPostadresse = new MidlertidigPostadresseNorge();

        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("SANNERGATA");
        gateadresse.setHusnummer(2);

        midlertidigPostadresse.gateadresse = gateadresse;
        midlertidigPostadresse.poststed = "0557";
        midlertidigPostadresse.land = new Land(NORGE);

        String json = mapper.writeValueAsString(midlertidigPostadresse);

        assertThat(json).isNotNull();
    }

    @Test
    void midlertidigPostadresseUtland() throws Exception {
        MidlertidigPostadresseUtland midlertidigPostadresse = new MidlertidigPostadresseUtland();
        midlertidigPostadresse.adresselinje1 = "42 Mock Road";
        midlertidigPostadresse.adresselinje2 = "Mock City";
        midlertidigPostadresse.adresselinje3 = "United Kingdom";
        midlertidigPostadresse.land = new Land(STORBRITANNIA);

        String json = mapper.writeValueAsString(midlertidigPostadresse);

        assertThat(json).isNotNull();
    }
}
