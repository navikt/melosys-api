package no.nav.melosys.integrasjon.sak

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SakConsumerProducer(@Value("\${SakAPI_v1.url}") private val endpointUrl: String) {
    @Bean
    fun sakConsumer(): SakConsumer {
        return SakConsumer(endpointUrl)
    }
}
