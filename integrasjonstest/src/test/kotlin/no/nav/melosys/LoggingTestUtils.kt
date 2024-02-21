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

    fun <T> Collection<T>.check(block: (next: () -> T) -> Unit) {
        var i = 0
        val list = toList()
        block { list[i++] }
    }


    inline fun <reified T : Any> Collection<ILoggingEvent>.match(): Collection<ILoggingEvent> {
        return filter { it.loggerName == T::class.java.name }
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

    class LogFilterBuilder(val logItems: Collection<ILoggingEvent>) {
         val result: MutableList<ILoggingEvent> = mutableListOf()

        inline fun <reified T : Any> match(predicate: (ILoggingEvent) -> Boolean = { true }): LogFilterBuilder =
            apply { result.addAll(logItems.filter { it.loggerName == T::class.java.name && predicate(it) }) }

        fun build(): Collection<ILoggingEvent> = result.sortedBy { it.timeStamp }
    }

    val Collection<ILoggingEvent>.filterBuilder: LogFilterBuilder
        get() = LogFilterBuilder(this)
}
