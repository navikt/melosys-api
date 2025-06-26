package no.nav.melosys.service

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executor

@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfig {
    @Bean("taskExecutor") // Separat fra saksflyt sin executor i ThreadPoolConfig
    fun taskExecutor(builder: ThreadPoolTaskExecutorBuilder): Executor {
        return builder.build()
    }
}
