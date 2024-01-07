package no.nav.melosys

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

object LoggingTestUtils {
    inline fun <reified T> captureLog(block: () -> Unit): List<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(T::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(listAppender)
        try {
            block()
        } finally {
            logger.detachAppender(listAppender)
        }
        return listAppender.list
    }

    fun <T> Collection<ILoggingEvent>.check(block: (next: () -> ILoggingEvent) -> T): T {
        var i = 0
        val iLoggingEventList = toList()
        return block { iLoggingEventList[i++] }
    }

    fun <T> withLogCapture(block: (logEvents: List<ILoggingEvent>) -> T): T {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val listAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(listAppender)
        try {
            return block(listAppender.list)
        } finally {
            logger.detachAppender(listAppender)
        }
    }
}
