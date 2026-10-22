package no.nav.melosys.integrasjon.kafka

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Mapper for meldinger melosys-api sender på Kafka.
 *
 * Den felles `JsonMapper`-en har `MelosysModule` fra `WebConfig`, som skriver kodeverk som
 * `{"kode", "term"}` til frontend. Mottakerne av Kafka-meldingene forventer koden som tekst.
 */
object KafkaProdusentJsonMapper {
    @JvmField
    val jsonMapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()
}
