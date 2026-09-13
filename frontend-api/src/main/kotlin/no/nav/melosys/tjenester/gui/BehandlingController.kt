package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.annotation.JsonView
import io.getunleash.Unleash
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.tjenester.gui.dto.BehandlingDto
import no.nav.melosys.tjenester.gui.dto.BehandlingOppsummeringDto
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/behandlinger")
@Tag(name = "behandlinger")
@Scope(WebApplicationContext.SCOPE_REQUEST)
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val saksopplysningerTilDto: SaksopplysningerTilDto,
    private val saksbehandlerService: SaksbehandlerService,
    private val aksesskontroll: Aksesskontroll,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val oppgaveService: OppgaveService,
    private val unleash: Unleash,
) {
    private val log = LoggerFactory.getLogger(BehandlingController::class.java)

    @PostMapping("{behandlingID}/tidligere-medlemsperioder")
    @Operation(summary = "Knytt medlemsperioder fra MEDL til oppholdsland fra søknaden")
    fun knyttMedlemsperioder(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody tidligereMedlemsperioder: TidligereMedlemsperioderDto,
    ): ResponseEntity<TidligereMedlemsperioderDto> {
        log.debug("Saksbehandler {} ber om å knytte medlemsperioder for behandling {}.", SubjectHandler.getInstance().userID, behandlingID)
        aksesskontroll.autoriserSkriv(behandlingID)
        behandlingService.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioder.periodeIder)
        return ResponseEntity.ok(tidligereMedlemsperioder)
    }

    @GetMapping("{behandlingID}/tidligere-medlemsperioder")
    @Operation(summary = "Hent medlemsperioder knyttet til oppholdsland fra søknaden")
    fun hentMedlemsperioder(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TidligereMedlemsperioderDto> {
        log.debug("Saksbehandler {} ber om å hente medlemsperioder for behandling {}.", SubjectHandler.getInstance().userID, behandlingID)
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(TidligereMedlemsperioderDto().apply {
            periodeIder = behandlingService.hentMedlemsperioder(behandlingID)
        })
    }

    @GetMapping("{behandlingID}")
    @JsonView(DokumentView.FrontendApi::class)
    @Operation(summary = "Hent en spesifikk behandling")
    fun hentBehandling(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<BehandlingDto> {
        val saksbehandlerID = SubjectHandler.getInstance().userID
        log.debug("Saksbehandler {} ber om å hente behandling {}.", saksbehandlerID, behandlingID)
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)
        val saksnummer = behandling.fagsak.saksnummer
        if (aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandlerID)) {
            aksesskontroll.auditAutoriserSkriv(behandlingID, "Saksbehandling og endringer for sak $saksnummer (behandling $behandlingID)")
        } else {
            aksesskontroll.auditAutoriser(behandlingID, "Innsyn i behandling $behandlingID på sak $saksnummer")
        }
        behandlingService.oppdaterBehandlingsstatusHvisTilhørendeSaksbehandler(behandling, saksbehandlerID)
        return ResponseEntity.ok(tilBehandlingDto(behandling, saksbehandlerID))
    }

    @GetMapping("{behandlingID}/mulige-statuser")
    @Operation(summary = "Hent mulige nye behandlingsstatuser for en behandling")
    fun hentMuligeStatuser(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Collection<Behandlingsstatus>> {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingsstatuser for behandling {}.", SubjectHandler.getInstance().userID, behandlingID)
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(behandlingService.hentMuligeStatuser(behandlingID))
    }

    private fun tilBehandlingDto(behandling: Behandling, saksbehandler: String): BehandlingDto {
        // Oppslaget koster et kall mot Oppgave-API, og gjøres bare når funksjonen er påskrudd.
        // Endepunktet gjør slike oppslag fra før, via aksesskontrollen og statusoppdateringen,
        // så dette skal ikke bli enda et kall for dem som ikke har funksjonen.
        val oppgave = if (unleash.isEnabled(ToggleName.MELOSYS_TILDEL_OPPGAVE)) {
            oppgaveService.finnBehandlingsoppgaveForBehandlingID(behandling.id)
        } else {
            null
        }
        val tilordnetIdent = oppgave?.tilordnetRessurs?.trim()?.takeIf { it.isNotEmpty() }
        val tilordnetMeg = tilordnetIdent != null && tilordnetIdent == saksbehandler
        return BehandlingDto(
            behandlingID = behandling.id,
            oppsummering = tilOppsummeringDto(behandling),
            saksopplysninger = saksopplysningerTilDto.getSaksopplysningerDto(behandling.saksopplysninger),
            redigerbart = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler),
            tilordnetIdent = tilordnetIdent,
            tilordnetNavn = finnNavnEllerIdent(tilordnetIdent),
            tilordnetMeg = tilordnetMeg,
            kanTildeles = oppgave != null && behandling.erRedigerbar() && !tilordnetMeg,
            tildelingTilgjengelig = oppgave != null,
        )
    }

    private fun finnNavnEllerIdent(ident: String?): String? {
        if (ident == null) return null
        return try {
            saksbehandlerService.finnNavnForIdent(ident).orElse(ident)
        } catch (e: RuntimeException) {
            log.warn("Klarte ikke hente saksbehandlernavn.", e)
            ident
        }
    }

    private fun tilOppsummeringDto(behandling: Behandling): BehandlingOppsummeringDto {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        return BehandlingOppsummeringDto().apply {
            behandlingsstatus = behandling.status
            behandlingstype = behandling.type
            behandlingstema = behandling.tema
            endretDato = behandling.endretDato
            endretAvNavn = finnNavnEllerIdent(behandling.endretAv)
            registrertDato = behandling.registrertDato
            sisteOpplysningerHentetDato = if (behandling.erEøsPensjonist() && behandling.saksopplysninger.isEmpty()) {
                null
            } else {
                behandling.sisteOpplysningerHentetDato
            }
            svarFrist = behandling.dokumentasjonSvarfristDato
            behandlingsfrist = behandling.behandlingsfrist
            behandlingsresultattype = behandlingsresultat.type
        }
    }
}
