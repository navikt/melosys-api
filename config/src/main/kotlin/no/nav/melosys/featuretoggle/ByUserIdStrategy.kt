package no.nav.melosys.featuretoggle

import io.getunleash.strategy.Strategy
import mu.KotlinLogging
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import java.util.*

internal class ByUserIdStrategy : Strategy {
    private val log = KotlinLogging.logger { }
    private val uniqueLogMessages = Collections.synchronizedSet(HashSet<String>())

    override fun getName(): String = "byUserId"

    override fun isEnabled(parameters: Map<String, String>): Boolean {
        val userIDs = parameters["user"]
        if (userIDs.isNullOrBlank()) return false

        val loggedInUserID = getLoggedInUserID()
        if (loggedInUserID.isNullOrBlank()) {
            logOnlyFirstMessage(
                "Unleash byUserId Strategy brukes uten innlogget saksbehandler, blir kalt fra:\n${calledFrom()}"
            )
            return false
        }

        return userIDs.split(",").contains(loggedInUserID)
    }

    private fun logOnlyFirstMessage(msg: String) {
        if (uniqueLogMessages.add(msg)) {
            log.warn(msg)
        }
    }

    private fun calledFrom(): String =
        Thread.currentThread().stackTrace.let { stackTraceElements ->
            val element = stackTraceElements.find {
                it.toString().contains(STACK_TRACE_LINE_BEFORE_UNLEASH_IS_ENABLED)
            } ?: return@let "Fant ikke unleash bruk i stacktrace\n" + stackTraceElements.joinToString("\n")
            val indexToLineAfterUnleashIsEnabledCall = stackTraceElements.indexOf(element)
            stackTraceElements[indexToLineAfterUnleashIsEnabledCall + 1].toString()
        }

    private fun getLoggedInUserID(): String? =
        SubjectHandler.getInstance()?.userID ?: ThreadLocalAccessInfo.getSaksbehandler()

    companion object {
        const val STACK_TRACE_LINE_BEFORE_UNLEASH_IS_ENABLED =
            "io.getunleash.DefaultUnleash.isEnabled(DefaultUnleash.java:93"
    }
}
