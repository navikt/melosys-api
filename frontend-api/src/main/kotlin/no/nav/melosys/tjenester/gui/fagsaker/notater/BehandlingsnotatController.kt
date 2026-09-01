package no.nav.melosys.tjenester.gui.fagsaker.notater

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.BehandlingsnotatService
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Protected
@RestController
@RequestMapping("/fagsaker")
@Tag(name = "fagsaker")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class BehandlingsnotatController(
    private val behandlingsnotatService: BehandlingsnotatService,
    private val saksbehandlerService: SaksbehandlerService,
    private val aksesskontroll: Aksesskontroll,
) {
    private val log = KotlinLogging.logger { }

    @GetMapping("/{saksnummer}/notater")
    @Operation(summary = "Henter alle notater knyttet til behandlinger for fagsaken")
    fun hentBehandlingsnotaterForFagsak(@PathVariable("saksnummer") saksnummer: String): ResponseEntity<Collection<BehandlingsnotatGetDto>> {
        aksesskontroll.autoriserSakstilgang(saksnummer)

        val notater = behandlingsnotatService.hentNotatForFagsak(saksnummer)
            .map(::lagBehandlingsnotatGetDto)

        return ResponseEntity.ok(notater)
    }

    @PostMapping("/{saksnummer}/notater")
    @Operation(summary = "Oppretter et nytt notat på fagsaken sin aktive behandling")
    fun opprettBehandlingsnotatForFagsak(
        @PathVariable("saksnummer") saksnummer: String,
        @RequestBody behandlingsnotatPostDto: BehandlingsnotatPostDto,
    ): ResponseEntity<BehandlingsnotatGetDto> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        val behandlingsnotat = behandlingsnotatService.opprettNotat(saksnummer, behandlingsnotatPostDto.tekst)
        return ResponseEntity.ok(lagBehandlingsnotatGetDto(behandlingsnotat))
    }

    @PutMapping("/{saksnummer}/notater/{notatID}")
    @Operation(summary = "Oppdaterer tekst på et notat")
    fun oppdaterBehandlingsnotat(
        @PathVariable("saksnummer") saksnummer: String,
        @PathVariable("notatID") notatID: Long,
        @RequestBody behandlingsnotatPostDto: BehandlingsnotatPostDto,
    ): ResponseEntity<BehandlingsnotatGetDto> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            lagBehandlingsnotatGetDto(behandlingsnotatService.oppdaterNotat(notatID, behandlingsnotatPostDto.tekst))
        )
    }

    private fun lagBehandlingsnotatGetDto(behandlingsnotat: Behandlingsnotat) = BehandlingsnotatGetDto(
        behandlingsnotat,
        behandlingsnotatService.kanRedigereNotat(behandlingsnotat),
        navnEllerIdent(behandlingsnotat.registrertAv),
    )

    private fun navnEllerIdent(ident: String): String = try {
        saksbehandlerService.finnNavnForIdent(ident).getOrNull() ?: ident
    } catch (e: TekniskException) {
        log.warn(e) { "Feil ved henting av navn for ident" }
        ident
    }
}

data class BehandlingsnotatPostDto(val tekst: String)

class BehandlingsnotatGetDto(
    behandlingsnotat: Behandlingsnotat,
    val redigerbar: Boolean,
    val registrertAvNavn: String,
) {
    val notatId: Long = behandlingsnotat.id
    val tekst: String? = behandlingsnotat.tekst
    val endretDato: Instant? = behandlingsnotat.endretDato
    val registrertDato: Instant? = behandlingsnotat.registrertDato
    val behandlingstypeKode: String = behandlingsnotat.behandling.type.kode
    val behandlingstemaKode: String = behandlingsnotat.behandling.tema.kode
}
