package no.nav.melosys.melosysmock.testdata

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/testdata")
class TestdataController(
    private val testDataGenerator: TestDataGenerator
) {

    @PostMapping("/jfr-oppgave")
    fun lagJournalføringsoppgave(@RequestBody request: OpprettJfrOppgaveRequest) {
        for (i in 0 until request.antall) {
            testDataGenerator.opprettJfrOppgave(request.tilordnetRessurs, request.forVirksomhet)
        }
    }

    data class OpprettJfrOppgaveRequest(
        val antall: Int,
        val tilordnetRessurs: String,
        val forVirksomhet: Boolean = false
    )
}
