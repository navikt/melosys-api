package no.nav.melosys.melosysmock.journalpost.journalpostapi

import no.nav.melosys.melosysmock.journalpost.JournalpostMapper
import no.nav.melosys.melosysmock.journalpost.JournalpostRepo.repo
import no.nav.melosys.melosysmock.journalpost.intern_modell.JournalStatus
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/rest/journalpostapi/v1")
@Unprotected
class JournalpostApi {

    @PostMapping("journalpost")
    fun opprettJournalpost(
        @RequestBody request: OpprettJournalpostRequest,
        @RequestParam("forsoekFerdigstill", required = false) forsoekFerdigstill: Boolean = false,
        @RequestParam("journalpostID", required = false) journalpostID: String
    ): Map<String, Any> {
        return JournalpostMapper().tilModell(request, forsoekFerdigstill, journalpostID)
            .also { repo[it.journalpostId] = it }
            .let {
                mapOf(
                    "journalpostId" to it.journalpostId,
                    "journalpostferdigstilt" to if (it.journalStatus == JournalStatus.J) "true" else "false",
                    "dokumenter" to it.dokumentModellList.map { d -> mapOf("dokumentInfoId" to d.dokumentId) }
                )
            }
    }

    @PutMapping("/journalpost/{journalpostID}")
    fun oppdaterJournalpost(
        @PathVariable("journalpostID") journalpostID: String,
        @RequestBody request: OppdaterJournalpostRequest
    ) {
        return (repo[journalpostID] ?: throw NoSuchElementException(""))
            .let { JournalpostMapper().oppdaterModell(request, it) }
            .also { repo[journalpostID] = it }
            .let { mapOf("journalpostId" to it.journalpostId) }
    }

    @PatchMapping("/journalpost/{journalpostID}/ferdigstill")
    fun ferdigstillJournalpost(
        @PathVariable("journalpostID") journalpostID: String,
        @RequestBody request: FerdigstillJournalpostRequest
    ) {
        val journalpost = repo[journalpostID] ?: throw NoSuchElementException("Ingen journalpost med id $journalpostID")
        journalpost.validerKanFerdigstilles()
        journalpost.journalfoertDato = LocalDateTime.now()
        journalpost.journalStatus = JournalStatus.J
    }
}
