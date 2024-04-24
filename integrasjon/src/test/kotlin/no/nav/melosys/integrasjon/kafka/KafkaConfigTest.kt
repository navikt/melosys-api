package no.nav.melosys.integrasjon.kafka

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

@JsonTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [KafkaConfig::class, KafkaProperties::class])
class KafkaConfigTest {
    @Autowired
    private lateinit var jsonDeserializer: JsonDeserializer<MelosysEessiMelding>

    private lateinit var meldingBytes: ByteArray

    @BeforeEach
    fun setUp() {
        val søknadURI = javaClass.classLoader.getResource("sed.json")?.toURI() ?: throw RuntimeException("Fant ikke sed.json")
        meldingBytes = Files.readAllBytes(Paths.get(søknadURI))
    }

    @Test
    fun parseMottattMelding() {
        val melding = jsonDeserializer.deserialize("topic", meldingBytes)


        melding.periode.run {
            fom shouldBeEqual LocalDate.of(2019, 8, 20)
            tom shouldBeEqual LocalDate.of(2022, 12, 20)
        }
    }
}
