package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Serializer


class MelosysHendelseSerializer : Serializer<MelosysHendelse> {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun serialize(topic: String, data: MelosysHendelse): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }

    override fun close() {}
}
