package no.nav.melosys

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

object LoggingTestUtils {
    inline fun <reified T> captureLog(block: () -> Unit): List<ILoggingEvent> {
        val listAppender = ListAppender<ILoggingEvent>().apply {
            start()
            (LoggerFactory.getLogger(T::class.java) as Logger).addAppender(this)
        }
        try {
            block()
        } finally {
            (LoggerFactory.getLogger(T::class.java) as Logger).detachAppender(listAppender)
        }
        return listAppender.list
    }

    fun <R> Collection<ILoggingEvent>.check(block: (next: () -> ILoggingEvent) -> R): R {
        var i = 0
        val iLoggingEventList = toList()
        return block { iLoggingEventList[i++] }
    }

    inline fun <reified T> withLogAppender(block: (list: List<ILoggingEvent>) -> Unit) {
        val listAppender = ListAppender<ILoggingEvent>().apply {
            start()
            (LoggerFactory.getLogger(T::class.java) as Logger).addAppender(this)
        }
        try {
            block(listAppender.list)
        } finally {
            (LoggerFactory.getLogger(T::class.java) as Logger).detachAppender(listAppender)
        }
    }

}
