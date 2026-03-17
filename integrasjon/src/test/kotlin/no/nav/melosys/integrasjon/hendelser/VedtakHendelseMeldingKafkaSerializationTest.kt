package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.LocalDate

/**
 * Verifiserer at VedtakHendelseMelding serialiseres korrekt for Kafka-produksjon.
 *
 * Bruker Spring Boot sin auto-konfigurerte ObjectMapper — identisk med ObjectMapper som
 * KafkaConfig injiserer i produksjon. Testen verifiserer at MelosysModule (KodeSerializer)
 * IKKE er aktiv for Kafka-serialisering: den er kun registrert på MVC-converters for
 * HTTP-responser til frontend. Hvis MelosysModule feilaktig registreres globalt vil
 * Kodeverk-enums serialiseres som {"kode":"...","term":"..."} i stedet for plain strings,
 * noe som brekker downstream consumers av vedtakshendelse-topicen.
 */
@JsonTest
class VedtakHendelseMeldingKafkaSerializationTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val vedtakHendelseMelding = VedtakHendelseMelding(
        folkeregisterIdent = "12345678901",
        sakstype = Sakstyper.TRYGDEAVTALE,
        sakstema = Sakstemaer.TRYGDEAVGIFT,
        behandligsresultatType = Behandlingsresultattyper.FERDIGBEHANDLET,
        vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK,
        medlemskapsperioder = listOf(
            Periode(
                fom = LocalDate.of(2021, 1, 1),
                tom = LocalDate.of(2022, 12, 31),
                innvilgelsesResultat = InnvilgelsesResultat.INNVILGET
            )
        ),
        lovvalgsperioder = listOf()
    )

    @Test
    fun `VedtakHendelseMelding serialiserer Kodeverk-enums som plain strings`() {
        val json = objectMapper.writeValueAsString(MelosysHendelse(vedtakHendelseMelding))

        json shouldEqualJson """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345678901",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT",
                    "behandligsresultatType": "FERDIGBEHANDLET",
                    "vedtakstype": "FØRSTEGANGSVEDTAK",
                    "medlemskapsperioder": [
                        {
                            "fom": "2021-01-01",
                            "tom": "2022-12-31",
                            "innvilgelsesResultat": "INNVILGET"
                        }
                    ],
                    "lovvalgsperioder": []
                }
            }
        """
    }

    @Test
    fun `VedtakHendelseMelding inneholder ikke kode-term-objekter for Kodeverk`() {
        val json = objectMapper.writeValueAsString(MelosysHendelse(vedtakHendelseMelding))

        // Verifiserer at KodeSerializer (MelosysModule) IKKE er aktiv for Kafka-serialisering
        json shouldNotContain """"kode""""
        json shouldNotContain """"term""""
    }

    @Test
    fun `VedtakHendelseMelding round-trip deserialisering`() {
        val original = MelosysHendelse(vedtakHendelseMelding)
        val json = objectMapper.writeValueAsString(original)
        val deserialized = objectMapper.readValue(json, MelosysHendelse::class.java)

        deserialized.melding shouldEqualVedtak vedtakHendelseMelding
    }

    private infix fun Any.shouldEqualVedtak(expected: VedtakHendelseMelding) {
        val actual = this.shouldBeInstanceOf<VedtakHendelseMelding>()
        actual shouldBe expected
    }
}
