package no.nav.melosys

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

object LoggingTestUtils {
    inline fun <reified T> withLogAppender(block: (listAppender: ListAppender<ILoggingEvent>) -> Unit) {
        val factorylogger = LoggerFactory.getLogger(T::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>().apply {
            context = factorylogger.loggerContext
        }
        try {
            listAppender.start()
            factorylogger.addAppender(listAppender)
            block(listAppender)
        } finally {
            factorylogger.detachAppender(listAppender)
        }
    }
}
