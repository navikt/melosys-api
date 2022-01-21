package no.nav.melosys.melosysmock.config

import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ws.config.annotation.EnableWs
import org.springframework.ws.transport.http.MessageDispatcherServlet

@EnableWs
@Configuration
class SoapConfig {
    @Bean
    fun messageDispatcherServlet(applicationContext: ApplicationContext): ServletRegistrationBean<MessageDispatcherServlet> {
        val servlet = MessageDispatcherServlet()
        servlet.setApplicationContext(applicationContext)
        servlet.isTransformWsdlLocations = true
        return ServletRegistrationBean(servlet, "/soap/*")
    }
}
