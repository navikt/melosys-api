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
        private var replacementsRegex = mutableListOf<Pair<Regex, String>>()
        private var replacementsString = mutableListOf<Pair<String, String>>()

        inline fun <reified T : Any> match(predicate: (ILoggingEvent) -> Boolean = { true }): LogFilterBuilder =
            apply { result.addAll(logItems.filter { it.loggerName == T::class.java.name && predicate(it) }) }

        fun remove(regex: Regex) = apply { this.regex = regex }
        fun replace(regex: Regex, replacement: String) = apply { replacementsRegex.add(regex to replacement) }
        fun replace(regex: String, replacement: String) = apply { replacementsString.add(regex to replacement) }

        fun build(): List<LogItem> {
            return logItems.filter { it in result }.map {
                LogItem(
                    message = it.message,
                    formattedMessage = resultMessage(it),
                    timeStamp =  it.timeStamp,
                    threadName =  it.threadName
                )
            }
        }

        private fun resultMessage(it: ILoggingEvent): String {
            var message = if (regex == null) it.formattedMessage else it.formattedMessage.replace(regex!!, "")
            replacementsRegex.forEach { (regex, replacement) ->
                message = message.replace(regex, replacement)
            }
            replacementsString.forEach { (text, replacement) ->
                message = message.replace(text, replacement)
            }
            return message
        }

        class LogItem(val message: String, val formattedMessage: String, val timeStamp: Long, val threadName: String)

        fun checkWithThreads(block: (next: (name: String) -> String) -> Unit) = apply {
            val map = mutableMapOf<String, Int>()
            val sorted = build()
            block { name ->
                val cnt = map[name] ?: 0
                map[name] = cnt + 1
                val threadMessages = sorted.filter { it.threadName == name }
                withClue("Thread $name, count $cnt:") {
                    threadMessages.shouldHaveAtLeastSize(cnt + 1)
                }
                threadMessages[cnt].formattedMessage
            }
        }

        fun check(block: (next: (nextLogItem: (message: String) -> Unit) -> Unit) -> Unit) = apply {
            val sorted = build()
            var i = 0
            block { nextLogItem ->
                withClue("log item count ${sorted.size} \n${sorted.joinToString("\n") { "${it.formattedMessage} - ${it.timeStamp}" }}\n") {
                    sorted.shouldHaveAtLeastSize(i + 1)
                }
                withClue("log message line: $i\n${sorted.joinToString("\n") { "${it.formattedMessage} - ${it.timeStamp}" }}\n") {
                    nextLogItem(sorted[i++].formattedMessage)
                }
            }
        }
    }

    val Collection<ILoggingEvent>.filterBuilder: LogFilterBuilder
        get() = LogFilterBuilder(this)
}
