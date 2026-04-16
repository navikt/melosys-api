package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Verifiserer at SkjemaApiTypeMixins fungerer korrekt med Jackson 3.
 *
 * Disse mixin-ene legger til @JsonCreator og @JsonTypeInfo på eksterne typer som IKKE har
 * no-arg konstruktører. Uten mixin-ene vil Jackson 3-deserialisering feile med
 * "no suitable constructor" – tilsvarende problemet som ble funnet i AnmodningsperiodePostDto.
 *
 * Tester kjøres via Prosessinstans.setData/getData som bruker den mixin-konfigurerte dataMapper internt.
 */
class SkjemaApiTypeMixinsTest {

    private val prosessinstans = Prosessinstans.forTest()

    @Test
    fun `SkjemaMottattMelding round-trip via Prosessinstans serialiserer og deserialiserer korrekt`() {
        // SkjemaMottattMelding har kun én konstruktør (UUID) – ingen no-arg konstruktør.
        // SkjemaMottattMeldingMixin legger til @JsonCreator for å muliggjøre Jackson 3-deserialisering.
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId)

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNAD_MOTTATT_MELDING, melding)
        val hentet = prosessinstans.getData(ProsessDataKey.DIGITAL_SØKNAD_MOTTATT_MELDING, SkjemaMottattMelding::class.java)

        hentet?.skjemaId shouldBe skjemaId
    }

    @Test
    fun `DegSelvMetadata round-trip via Prosessinstans serialiserer og deserialiserer korrekt`() {
        // UtsendtArbeidstakerMetadata er polymorf med @JsonTypeInfo-mixin.
        // DegSelvMetadata har ingen no-arg konstruktør – @JsonCreator-mixin er påkrevd for deserialisering.
        val metadata = DegSelvMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Testbedrift AS",
            juridiskEnhetOrgnr = "123456789",
            kobletSkjemaId = null,
            erstatterSkjemaId = null,
        )

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNAD_MOTTATT_MELDING, metadata)
        val hentet = prosessinstans.getData(ProsessDataKey.DIGITAL_SØKNAD_MOTTATT_MELDING, DegSelvMetadata::class.java)

        hentet.shouldBeInstanceOf<DegSelvMetadata>()
        hentet.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
        hentet.arbeidsgiverNavn shouldBe "Testbedrift AS"
        hentet.juridiskEnhetOrgnr shouldBe "123456789"
    }
}
