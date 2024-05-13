package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import mu.KotlinLogging
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.oppgave.OppgaveSoekFilter
import no.nav.melosys.service.oppgave.Oppgaveplukker
import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto
import no.nav.melosys.service.oppgave.dto.JournalfoeringsoppgaveDto
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveOversiktDto
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto
import no.nav.security.token.support.core.api.Protected
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/oppgaver")
@Api(tags = ["oppgaver"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class OppgaveTjeneste(
    private val oppgaveplukker: Oppgaveplukker,
    private val oppgaveService: OppgaveService,
    private val oppgaveSoekFilter: OppgaveSoekFilter
) {
    private val log = KotlinLogging.logger { }

    @PostMapping("/plukk")
    @ApiOperation(
        value = "Plukker neste oppgave fra Oppgave som saksbehandler skal arbeide med.",
        response = PlukketOppgaveDto::class
    )
    fun plukkOppgave(@RequestBody plukkDto: PlukkOppgaveInnDto): ResponseEntity<PlukketOppgaveDto> {
        val ident = SubjectHandler.getInstance().getUserID()
        val plukketOppgave = oppgaveplukker.plukkOppgave(ident, plukkDto)
        return if (plukketOppgave != null) {
            val behandling = oppgaveService.hentSistAktiveBehandling(plukketOppgave.saksnummer)
            ResponseEntity.ok(
                PlukketOppgaveDto(
                    oppgaveID = plukketOppgave.oppgaveId,
                    behandlingID = behandling.id,
                    behandlingstype = behandling.type.kode,
                    behandlingstema = behandling.tema.kode,
                    journalpostID = plukketOppgave.journalpostId,
                    saksnummer = finnSaksnummer(plukketOppgave)
                )
            )
        } else {
            ResponseEntity.ok(
                PlukketOppgaveDto(
                    antallUtildelteOppgaver = oppgaveplukker.hentUtildelteOppgaver(plukkDto).size
                )
            )
        }
    }

    @PostMapping("/tilbakelegg")
    @ApiOperation(value = "Legger tilbake oppgave knyttet til gitt behandlingID i GSAK.")
    fun leggTilbakeOppgave(@RequestBody tilbakelegging: TilbakeleggingDto): ResponseEntity<Void> {
        val ident = SubjectHandler.getInstance().getUserID()
        oppgaveplukker.leggTilbakeOppgave(ident, tilbakelegging)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/oversikt")
    @ApiOperation(
        value = "Henter alle oppgaver som er tildelt innlogget saksbehandler.",
        response = OppgaveOversiktDto::class
    )
    fun mineOppgaver(): ResponseEntity<OppgaveOversiktDto> {
        val ident = SubjectHandler.getInstance().getUserID()
        val journalføring = ArrayList<JournalfoeringsoppgaveDto>()
        val saksbehandling = ArrayList<BehandlingsoppgaveDto>()

        for (oppgaveDto in oppgaveService.hentOppgaverMedAnsvarlig(ident)) {
            when (oppgaveDto) {
                is JournalfoeringsoppgaveDto -> journalføring.add(oppgaveDto)
                is BehandlingsoppgaveDto -> saksbehandling.add(oppgaveDto)
                else -> log.warn("Ukjent oppgavetype: ${oppgaveDto.javaClass.getSimpleName()}")
            }
        }

        return ResponseEntity.ok(
            OppgaveOversiktDto(
                journalforing = journalføring,
                saksbehandling = saksbehandling
            )
        )
    }

    @GetMapping("/sok")
    @ApiOperation(
        value = "Søk etter oppgaver knyttet til et fødselsnummer, d-nummer, eller organisasjonsnummer",
        response = OppgaveDto::class,
        responseContainer = "List"
    )
    fun søkOppgaverMedPersonIdentEllerOrgnr(
        @RequestParam(name = "personIdent", required = false) personIdent: String?,
        @RequestParam(name = "orgnr", required = false) orgnr: String?
    ): ResponseEntity<List<OppgaveDto>> {
        if (personIdent.isNullOrEmpty() && orgnr.isNullOrEmpty()) {
            throw FunksjonellException("Finner ingen søkekriteria. API støtter personIdent(fnr eller dnr) og orgnr")
        }
        if (StringUtils.isNotEmpty(personIdent) && StringUtils.isNotEmpty(orgnr)) {
            throw FunksjonellException("Fant både personIdent og orgnr. API støtter kun én.")
        }
        return try {
            val oppgaveliste =
                if (orgnr.isNullOrEmpty()) oppgaveSoekFilter.finnBehandlingsoppgaverMedPersonIdent(personIdent!!)
                else oppgaveSoekFilter.finnBehandlingsoppgaverMedOrgnr(orgnr)
            ResponseEntity.ok(oppgaveliste.map { OppgaveDto.av(it) })
        } catch (e: IkkeFunnetException) {
            ResponseEntity.ok(ArrayList())
        }
    }

    private fun finnSaksnummer(oppgave: Oppgave): String? {
        val conditions = listOf(
            Oppgave::erBehandling,
            Oppgave::erVurderDokument,
            Oppgave::erSedBehandling,
            Oppgave::erVurderHenvendelse,
            Oppgave::erManglendeInnbetalingBehandling
        )
        return if (conditions.any { it(oppgave) })
            oppgave.saksnummer
        else
            null
    }
}
