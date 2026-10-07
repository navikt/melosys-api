package no.nav.melosys.saksflytapi.domain

import com.fasterxml.jackson.annotation.JsonProperty
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
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

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID, melding)
        val hentet = prosessinstans.getData(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID, SkjemaMottattMelding::class.java)

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
            arbeidstakerNavn = "Test Arbeidstaker",
            kobletSkjemaId = null,
            erstatterSkjemaId = null,
        )

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID, metadata)
        val hentet = prosessinstans.getData(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID, DegSelvMetadata::class.java)

        hentet.shouldBeInstanceOf<DegSelvMetadata>()
        hentet.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
        hentet.arbeidsgiverNavn shouldBe "Testbedrift AS"
        hentet.juridiskEnhetOrgnr shouldBe "123456789"
        hentet.arbeidstakerNavn shouldBe "Test Arbeidstaker"
    }

    @Test
    fun `eldre lagret skjema uten nye felt bruker bakoverkompatible standardverdier`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto()
        val json = Prosessinstans.dataMapper.valueToTree<JsonNode>(dto)
        fjernFelterRekursivt(json, "skjemaDefinisjonVersjon", "erOffentligArbeidsgiver")

        val hentet = Prosessinstans.dataMapper.treeToValue(json, UtsendtArbeidstakerSkjemaM2MDto::class.java)

        hentet.skjema.skjemaDefinisjonVersjon shouldBe "1"
        hentet.skjema.metadata.erOffentligArbeidsgiver shouldBe null
    }

    @Test
    fun `antallAnsatte fra EREG overlever round-trip av søknadsdata via Prosessinstans`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto { antallAnsatte = 7 }

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNADSDATA, dto)
        val hentet = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)

        hentet.skjema.metadata.antallAnsatte shouldBe 7
    }

    @Test
    fun `metadata-mixins har samme konstruktørparametre som typene fra melosys-skjema-api-types`() {
        // Uten lik signatur ignoreres @JsonCreator-mixinen, og nye felter i typene blir ikke fanget opp her
        mapOf(
            DegSelvMetadata::class to DegSelvMetadataMixin::class,
            ArbeidsgiverMetadata::class to ArbeidsgiverMetadataMixin::class,
            AnnenPersonMetadata::class to AnnenPersonMetadataMixin::class,
            ArbeidsgiverMedFullmaktMetadata::class to ArbeidsgiverMedFullmaktMetadataMixin::class,
            RadgiverMetadata::class to RadgiverMetadataMixin::class,
            RadgiverMedFullmaktMetadata::class to RadgiverMedFullmaktMetadataMixin::class,
        ).forEach { (type, mixin) ->
            val mixinParametre = mixin.java.declaredConstructors.single().parameters.map {
                it.getAnnotation(JsonProperty::class.java).value to it.type
            }
            val typeParametre = requireNotNull(type.primaryConstructor).parameters.map {
                it.name to it.type.jvmErasure.javaObjectType
            }
            withClue(mixin.simpleName) {
                mixinParametre.map { (navn, klasse) -> navn to klasse.kotlin.javaObjectType } shouldBe typeParametre
            }
        }
    }

    private fun fjernFelterRekursivt(node: JsonNode, vararg feltnavn: String) {
        if (node is ObjectNode) {
            feltnavn.forEach { node.remove(it) }
        }
        node.forEach { fjernFelterRekursivt(it, *feltnavn) }
    }
}
