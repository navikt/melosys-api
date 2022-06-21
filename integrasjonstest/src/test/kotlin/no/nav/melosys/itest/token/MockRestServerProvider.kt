package no.nav.melosys.itest.token

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

@Configuration
class MockRestServerProvider(
    @Autowired private val applicationContext: ApplicationContext
) {
    private val customizer = MockServerRestTemplateCustomizer()

    @Bean
    fun provideBuilder(): RestTemplateBuilder {
        return RestTemplateBuilder(customizer)
    }

    fun getStsRestTemplate() = applicationContext
        .autowireCapableBeanFactory
        .getBean("stsRestTemplate") as RestTemplate

    fun getSecurityMock(): MockRestServiceServer {
        val stsRestTemplate = getStsRestTemplate()
        return customizer.servers[stsRestTemplate]!!
    }

    fun getServiceUnderTestMockServer(): MockRestServiceServer {
        val stsRestTemplate = getStsRestTemplate()
        return customizer.servers
            .filterKeys { restTemplate -> restTemplate != stsRestTemplate }
            .values
            .first()!!
    }

    fun reset() {
        getSecurityMock().reset()
        if (customizer.servers.size > 1) {
            getServiceUnderTestMockServer().reset()
        }
    }
}
