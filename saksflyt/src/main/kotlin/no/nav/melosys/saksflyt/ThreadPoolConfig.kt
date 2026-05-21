package no.nav.melosys.saksflyt

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class ThreadPoolConfig {

    @Bean(name = ["saksflytThreadPoolTaskExecutor"])
    fun saksflytThreadPoolTaskExecutor(): ThreadPoolTaskExecutor =
        PrioritertSaksflytTaskExecutor().apply {
            corePoolSize = 3
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(20)
            afterPropertiesSet()
        }
}
