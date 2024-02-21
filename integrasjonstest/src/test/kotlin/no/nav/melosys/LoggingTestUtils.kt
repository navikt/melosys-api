package no.nav.melosys

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
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
        private var regex: Regex? = null

        inline fun <reified T : Any> match(predicate: (ILoggingEvent) -> Boolean = { true }): LogFilterBuilder =
            apply { result.addAll(logItems.filter { it.loggerName == T::class.java.name && predicate(it) }) }

        fun remove(regex: Regex) = apply { this.regex = regex }

        fun build(): Collection<ILoggingEvent> {
            return result.sortedBy { it.timeStamp }
        }

        fun checkWithThreads(block: (next: (name: String) -> String) -> Unit) {
            val map = mutableMapOf<String, Int>()
            val sorted = result.sortedBy { it.timeStamp }
            block { name ->
                val cnt = map[name] ?: 0
                map[name] = cnt + 1
                val threadMessages = sorted.filter { it.threadName == name }
                withClue("Thread $name, count $cnt:") {
                    threadMessages.shouldHaveAtLeastSize(cnt+1)
                }
                val message: String = threadMessages[cnt].formattedMessage
                if(regex == null) message else message.replace(regex!!, "")
            }
        }

    }

    val Collection<ILoggingEvent>.filterBuilder: LogFilterBuilder
        get() = LogFilterBuilder(this)
}
