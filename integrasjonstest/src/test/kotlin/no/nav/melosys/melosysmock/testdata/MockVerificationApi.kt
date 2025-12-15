package no.nav.melosys.melosysmock.testdata

import no.nav.melosys.melosysmock.journalpost.JournalpostRepo
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiApi.SaksrelasjonLager.saksrelasjoner
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiRepo
import no.nav.melosys.melosysmock.oppgave.OppgaveRepo
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * REST API for verifying mock state in integration tests.
 * These endpoints allow tests to verify what data was created in the mock
 * without coupling to internal repo implementation.
 */
@RestController
@RequestMapping("/testdata")
@Unprotected
class MockVerificationApi(
    private val oppgaveRepo: OppgaveRepo,
    private val journalpostRepo: JournalpostRepo
) {

    // ========== MEDL ==========

    @GetMapping("/verification/medl")
    fun getMedl(): List<MedlVerificationDto> = MedlRepo.repo.values.map { medl ->
        MedlVerificationDto(
            unntakId = medl.unntakId,
            ident = medl.ident,
            fraOgMed = medl.fraOgMed,
            tilOgMed = medl.tilOgMed,
            status = medl.status,
            dekning = medl.dekning,
            lovvalgsland = medl.lovvalgsland,
            lovvalg = medl.lovvalg,
            grunnlag = medl.grunnlag,
            medlem = medl.medlem,
            sporingsinformasjon = medl.sporingsinformasjon?.let { sporing ->
                SporingsinformasjonVerificationDto(
                    versjon = sporing.versjon,
                    registrert = sporing.registrert,
                    besluttet = sporing.besluttet,
                    kilde = sporing.kilde,
                    kildedokument = sporing.kildedokument,
                    opprettet = sporing.opprettet,
                    opprettetAv = sporing.opprettetAv,
                    sistEndret = sporing.sistEndret,
                    sistEndretAv = sporing.sistEndretAv
                )
            }
        )
    }

    @GetMapping("/verification/medl/count")
    fun getMedlCount(): Int = MedlRepo.repo.size

    // ========== SAK ==========

    @GetMapping("/verification/sak")
    fun getSak(): List<SakVerificationDto> = SakRepo.repo.values.map { sak ->
        SakVerificationDto(
            id = sak.id,
            tema = sak.tema,
            applikasjon = sak.applikasjon,
            fagsakNr = sak.fagsakNr,
            aktoerId = sak.aktoerId
        )
    }

    @GetMapping("/verification/sak/count")
    fun getSakCount(): Int = SakRepo.repo.size

    @GetMapping("/verification/sak/fagsak/{fagsakNr}")
    fun getSakByFagsakNr(@PathVariable fagsakNr: String): ResponseEntity<SakVerificationDto> {
        val sak = SakRepo.fagsakNrSakRepo[fagsakNr]
        return if (sak != null) {
            ResponseEntity.ok(
                SakVerificationDto(
                    id = sak.id,
                    tema = sak.tema,
                    applikasjon = sak.applikasjon,
                    fagsakNr = sak.fagsakNr,
                    aktoerId = sak.aktoerId
                )
            )
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ========== OPPGAVE ==========

    @GetMapping("/verification/oppgave")
    fun getOppgave(): List<OppgaveVerificationDto> = oppgaveRepo.repo.values.map { it.toVerificationDto() }

    @GetMapping("/verification/oppgave/count")
    fun getOppgaveCount(): Int = oppgaveRepo.repo.size

    @GetMapping("/verification/oppgave/type/{oppgavetype}")
    fun getOppgaveByType(@PathVariable oppgavetype: String): List<OppgaveVerificationDto> =
        oppgaveRepo.repo.values
            .filter { it.oppgavetype == oppgavetype }
            .map { it.toVerificationDto() }

    private fun no.nav.melosys.melosysmock.oppgave.Oppgave.toVerificationDto() = OppgaveVerificationDto(
        id = id,
        aktivDato = aktivDato,
        aktoerId = aktoerId,
        behandlesAvApplikasjon = behandlesAvApplikasjon,
        behandlingstema = behandlingstema,
        behandlingstype = behandlingstype,
        beskrivelse = beskrivelse,
        endretTidspunkt = endretTidspunkt,
        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
        fristFerdigstillelse = fristFerdigstillelse,
        journalpostId = journalpostId,
        oppgavetype = oppgavetype,
        opprettetTidspunkt = opprettetTidspunkt,
        orgnr = orgnr,
        prioritet = prioritet,
        saksreferanse = saksreferanse,
        status = status,
        tema = tema,
        tildeltEnhetsnr = tildeltEnhetsnr,
        tilordnetRessurs = tilordnetRessurs,
        versjon = versjon,
        metadata = metadata?.mapKeys { it.key.name }
    )

    // ========== JOURNALPOST ==========

    @GetMapping("/verification/journalpost")
    fun getJournalpost(): List<JournalpostVerificationDto> = journalpostRepo.repo.values.map { it.toVerificationDto() }

    @GetMapping("/verification/journalpost/count")
    fun getJournalpostCount(): Int = journalpostRepo.repo.size

    @GetMapping("/verification/journalpost/{journalpostId}")
    fun getJournalpostById(@PathVariable journalpostId: String): ResponseEntity<JournalpostVerificationDto> {
        val jp = journalpostRepo.repo[journalpostId]
        return if (jp != null) {
            ResponseEntity.ok(jp.toVerificationDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/verification/journalpost/sak/{saksnummer}")
    fun getJournalpostBySak(@PathVariable saksnummer: String): List<JournalpostVerificationDto> =
        journalpostRepo.finnVedSaksnummer(saksnummer).map { it.toVerificationDto() }

    private fun no.nav.melosys.melosysmock.journalpost.intern_modell.JournalpostModell.toVerificationDto() =
        JournalpostVerificationDto(
            journalpostId = journalpostId,
            journalStatus = journalStatus?.name,
            journalposttype = journalposttype?.name,
            tittel = tittel,
            arkivtema = arkivtema?.name,
            kanal = kanal,
            mottattDato = mottattDato
        )

    // ========== MELOSYS-EESSI ==========

    @GetMapping("/verification/melosys-eessi/sed/{rinaSaksnummer}")
    fun getSedForBuc(@PathVariable rinaSaksnummer: String): List<String> =
        MelosysEessiRepo.sedRepo[rinaSaksnummer]?.map { it.name } ?: emptyList()

    @GetMapping("/verification/melosys-eessi/sed")
    fun getAllSedRepo(): Map<String, List<String>> =
        MelosysEessiRepo.sedRepo.mapValues { (_, sedTypes) -> sedTypes.map { it.name } }

    @GetMapping("/verification/melosys-eessi/buc")
    fun getBuc(): List<BucVerificationDto> = getBucer()

    @GetMapping("/verification/melosys-eessi/bucer")
    fun getBucer(): List<BucVerificationDto> = MelosysEessiRepo.repo.map { buc ->
        BucVerificationDto(
            id = buc.id,
            erAapen = buc.erÅpen(),
            bucType = buc.bucType,
            opprettetDato = buc.opprettetDato,
            mottakerinstitusjoner = buc.mottakerinstitusjoner,
            seder = buc.seder.map { sed ->
                SedVerificationDto(
                    bucId = sed.bucId,
                    sedId = sed.sedId,
                    opprettetDato = sed.opprettetDato,
                    sistOppdatert = sed.sistOppdatert,
                    sedType = sed.sedType,
                    status = sed.status,
                    rinaUrl = sed.rinaUrl
                )
            }
        )
    }

    @GetMapping("/verification/melosys-eessi/bucer/count")
    fun getBucerCount(): Int = MelosysEessiRepo.repo.size

    @GetMapping("/verification/melosys-eessi/saksrelasjoner")
    fun getSaksrelasjoner(): List<SaksrelasjonVerificationDto> = saksrelasjoner.map { sr ->
        SaksrelasjonVerificationDto(
            gsakSaksnummer = sr.gsakSaksnummer,
            rinaSaksnummer = sr.rinaSaksnummer,
            bucType = sr.bucType
        )
    }

    @GetMapping("/verification/melosys-eessi/saksrelasjoner/count")
    fun getSaksrelasjonerCount(): Int = saksrelasjoner.size

    @GetMapping("/verification/melosys-eessi/sedrepo/count")
    fun getSedRepoCount(): Int = MelosysEessiRepo.sedRepo.size

    // ========== SUMMARY ==========

    @GetMapping("/verification/summary")
    fun getSummary(): MockSummaryDto = MockSummaryDto(
        medlCount = MedlRepo.repo.size,
        sakCount = SakRepo.repo.size,
        oppgaveCount = oppgaveRepo.repo.size,
        journalpostCount = journalpostRepo.repo.size,
        bucCount = MelosysEessiRepo.repo.size,
        sedRepoCount = MelosysEessiRepo.sedRepo.size,
        saksrelasjonCount = saksrelasjoner.size
    )

    // ========== CLEAR ==========

    @DeleteMapping("/clear")
    fun clearAll(): ClearResponse {
        MedlRepo.repo.clear()
        SakRepo.clear()
        oppgaveRepo.repo.clear()
        journalpostRepo.repo.clear()
        MelosysEessiRepo.clear()
        saksrelasjoner.clear()

        return ClearResponse(
            message = "All mock data cleared",
            medlCleared = "true",
            sakCleared = "true",
            oppgaveCleared = "true",
            journalpostCleared = "true",
            melosysEessiCleared = "true"
        )
    }
}

// ========== DTOs ==========

data class MedlVerificationDto(
    val unntakId: Long? = null,
    val ident: String? = null,
    val fraOgMed: LocalDate? = null,
    val tilOgMed: LocalDate? = null,
    val status: String? = null,
    val dekning: String? = null,
    val lovvalgsland: String? = null,
    val lovvalg: String? = null,
    val grunnlag: String? = null,
    val medlem: Boolean? = null,
    val sporingsinformasjon: SporingsinformasjonVerificationDto? = null
)

data class SporingsinformasjonVerificationDto(
    val versjon: Int? = null,
    val registrert: LocalDate? = null,
    val besluttet: LocalDate? = null,
    val kilde: String? = null,
    val kildedokument: String? = null,
    val opprettet: java.time.LocalDateTime? = null,
    val opprettetAv: String? = null,
    val sistEndret: java.time.LocalDateTime? = null,
    val sistEndretAv: String? = null
)

data class SakVerificationDto(
    val id: Long? = null,
    val tema: String? = null,
    val applikasjon: String? = null,
    val fagsakNr: String? = null,
    val aktoerId: String? = null
)

data class OppgaveVerificationDto(
    val id: Int? = null,
    val aktivDato: LocalDate? = null,
    val aktoerId: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val beskrivelse: String? = null,
    val endretTidspunkt: java.time.LocalDateTime? = null,
    val ferdigstiltTidspunkt: java.time.LocalDateTime? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val journalpostId: String? = null,
    val oppgavetype: String? = null,
    val opprettetTidspunkt: java.time.ZonedDateTime? = null,
    val orgnr: String? = null,
    val prioritet: String? = null,
    val saksreferanse: String? = null,
    val status: String? = null,
    val tema: String? = null,
    val tildeltEnhetsnr: String? = null,
    val tilordnetRessurs: String? = null,
    val versjon: Int? = null,
    val metadata: Map<String, String>? = null
)

data class JournalpostVerificationDto(
    val journalpostId: String? = null,
    val journalStatus: String? = null,
    val journalposttype: String? = null,
    val tittel: String? = null,
    val arkivtema: String? = null,
    val kanal: String? = null,
    val mottattDato: LocalDate? = null
)

data class BucVerificationDto(
    val id: String? = null,
    val erAapen: Boolean? = null,
    val bucType: String? = null,
    val opprettetDato: LocalDate? = null,
    val mottakerinstitusjoner: Set<String>? = null,
    val seder: List<SedVerificationDto>? = null
)

data class SedVerificationDto(
    val bucId: String? = null,
    val sedId: String? = null,
    val opprettetDato: LocalDate? = null,
    val sistOppdatert: LocalDate? = null,
    val sedType: String? = null,
    val status: String? = null,
    val rinaUrl: String? = null
)

data class SaksrelasjonVerificationDto(
    val gsakSaksnummer: Long? = null,
    val rinaSaksnummer: String? = null,
    val bucType: String? = null
)

data class MockSummaryDto(
    val medlCount: Int = 0,
    val sakCount: Int = 0,
    val oppgaveCount: Int = 0,
    val journalpostCount: Int = 0,
    val bucCount: Int = 0,
    val sedRepoCount: Int = 0,
    val saksrelasjonCount: Int = 0
)

data class ClearResponse(
    val message: String? = null,
    val journalpostCleared: String? = null,
    val oppgaveCleared: String? = null,
    val medlCleared: String? = null,
    val sakCleared: String? = null,
    val melosysEessiCleared: String? = null
)
