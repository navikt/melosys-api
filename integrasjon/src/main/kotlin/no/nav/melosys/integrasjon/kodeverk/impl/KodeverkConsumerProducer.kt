package no.nav.melosys.integrasjon.kodeverk.impl

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KodeverkConsumerProducer(@param:Value("\${KodeverkAPI_v1.url}") private val endpointUrl: String) {
    @Bean
    fun kodeverkConsumer(): KodeverkConsumerImpl {
        return KodeverkConsumerImpl(endpointUrl)
    }
}
