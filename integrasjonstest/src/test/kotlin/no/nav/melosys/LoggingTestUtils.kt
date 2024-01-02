package no.nav.melosys

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

object LoggingTestUtils {
    inline fun <reified T> withLogAppender(block: (listAppender: ListAppender<ILoggingEvent>) -> Unit) {
        val factorylogger = LoggerFactory.getLogger(T::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>().apply {
            context = factorylogger.loggerContext
        }
        try {
            listAppender.start()
            factorylogger.addAppender(listAppender)
            listAppender.list
            block(listAppender)
        } finally {
            factorylogger.detachAppender(listAppender)
        }
    }

    inline fun withLogAppender(vararg classes: KClass<*>, block: () -> Unit): List<ILoggingEvent> {
        val listAppenders = classes.map {
            ListAppender<ILoggingEvent>().apply {
                context = (LoggerFactory.getLogger(it.java) as Logger).loggerContext
                start()
                (LoggerFactory.getLogger(it.java) as Logger).addAppender(this)
            }
        }

        try {
            block()
        } finally {
            listAppenders.forEach { appender ->
                (LoggerFactory.getLogger(appender.context.name) as Logger).detachAppender(appender)
                appender.stop()
            }
        }

        return listAppenders.flatMap { it.list }.sortedBy { it.timeStamp }
    }
}
