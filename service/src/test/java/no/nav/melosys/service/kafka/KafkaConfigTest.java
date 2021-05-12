package no.nav.melosys.service.kafka;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KafkaConfig.class, KafkaProperties.class })
class KafkaConfigTest {

    @Autowired
    private JsonDeserializer<MelosysEessiMelding> jsonDeserializer;

    private byte[] meldingBytes;

    @BeforeEach
    public void setUp() throws Exception {
        URI søknadURI = (getClass().getClassLoader().getResource("sed.json")).toURI();
        meldingBytes = Files.readAllBytes(Paths.get(søknadURI));
    }

    @Test
    void parseMottattMelding() {
        MelosysEessiMelding melding = jsonDeserializer.deserialize("topic", meldingBytes);

        assertThat(melding.getPeriode().getFom()).isEqualTo(LocalDate.of(2019, 8, 20));
        assertThat(melding.getPeriode().getTom()).isEqualTo(LocalDate.of(2022, 12, 20));
    }
}
