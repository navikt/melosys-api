package no.nav.melosys.tjenester.gui.fagsaker

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.FTRL
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ÅRSAVREGNING
import no.nav.melosys.domain.util.MottatteOpplysningerUtils
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.sak.*
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.tjenester.gui.dto.*
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import no.nav.security.token.support.core.api.Protected
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext
import kotlin.jvm.optionals.getOrNull

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = ["fagsaker"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class FagsakController(
    private val fagsakService: FagsakService,
    private val aksesskontroll: Aksesskontroll,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val opprettSak: OpprettSak,
    private val endreSakService: EndreSakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val persondataFasade: PersondataFasade,
    private val saksopplysningerService: SaksopplysningerService,
    private val organisasjonOppslagService: OrganisasjonOppslagService,
    private val opprettBehandlingForSak: OpprettBehandlingForSak,
    private val ferdigbehandleService: FerdigbehandleService
) {
    private val log = KotlinLogging.logger { }
    private val UKJENT_NAVN = "UKJENT"

    @GetMapping("/{saksnr}")
    @ApiOperation(
        value = "Henter en sak med et gitt saksnummer",
        notes = ("Spesifikke saker kan hentes via saksnummer.")
    )
    fun hentFagsak(@PathVariable("saksnr") saksnummer: String): ResponseEntity<FagsakDto> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        aksesskontroll.autoriserSakstilgang(fagsak)
        val fagsakDto = tilFagsakDto(fagsak)
        log.info("Henting av sak {} ({})", fagsakDto.saksnummer, fagsakDto.gsakSaksnummer)
        return ResponseEntity.ok(fagsakDto)
    }

    @PostMapping
    @ApiOperation(value = "Oppretter en ny sak.")
    fun opprettNySak(@RequestBody opprettSakDto: OpprettSakDto): ResponseEntity<Void> {
        if (opprettSakDto.brukerID == null && opprettSakDto.virksomhetOrgnr == null) {
            throw FunksjonellException("BrukerID eller organisasjonsnummer trengs for å opprette en sak.")
        }

        opprettSakDto.brukerID?.let { aksesskontroll.autoriserFolkeregisterIdent(it) }

        if (opprettSakDto.oppgaveID == null) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        } else {
            opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto)
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{saksnr}/behandlinger")
    @ApiOperation(value = "Oppretter en ny behandling for sak.")
    fun opprettNyBehandlingForSak(
        @PathVariable("saksnr") saksnummer: String?,
        @RequestBody opprettSakDto: OpprettSakDto
    ): ResponseEntity<Void> {
        opprettSakDto.brukerID?.let { aksesskontroll.autoriserFolkeregisterIdent(it) }
        opprettBehandlingForSak.opprettBehandling(saksnummer, opprettSakDto)

        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{saksnr}")
    @ApiOperation(value = "Endre en sak.")
    fun endreFagsak(
        @PathVariable("saksnr") saksnummer: String,
        @RequestBody endreDto: EndreSakDto
    ): ResponseEntity<Void> {
        log.debug(
            "Saksbehandler {} ber om å endre fagsak {} med sakstype {}, sakstema {}",
            SubjectHandler.getInstance().getUserID(), saksnummer, endreDto.sakstype, endreDto.sakstema
        )
        aksesskontroll.autoriserSakstilgang(saksnummer)

        if (endreDto.behandlingstype == ÅRSAVREGNING && endreDto.behandlingID != null) {
            endreSakService.endreÅrsavregningBehandling(
                endreDto.behandlingID!!,
                endreDto.behandlingsstatus,
                endreDto.mottaksdato
            )
        } else {
            endreSakService.endre(
                saksnummer, endreDto.sakstype, endreDto.sakstema, endreDto.behandlingstema,
                endreDto.behandlingstype, endreDto.behandlingsstatus, endreDto.mottaksdato
            )
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/sok")
    @ApiOperation(
        value = "Søk etter saker på ident eller saksnummer eller orgnr",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer. Saker knyttet til en organisasjon søkes via organisasjonsnummer."),
        response = FagsakOppsummeringDto::class,
        responseContainer = "List"
    )
    fun hentFagsaker(@RequestBody fagsakSokDto: FagsakSokDto): List<FagsakOppsummeringDto> = when {
        StringUtils.isNotEmpty(fagsakSokDto.ident) -> {
            aksesskontroll.auditAutoriserFolkeregisterIdent(
                fagsakSokDto.ident, "Søk på person med ident. Oversikt over saker og behandlinger."
            )
            tilFagsakOppsummeringDtoer(fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, fagsakSokDto.ident))
        }

        StringUtils.isNotEmpty(fagsakSokDto.saksnummer) -> {
            val optionalFagsak = fagsakService.finnFagsakFraSaksnummer(fagsakSokDto.saksnummer)
            if (optionalFagsak.isPresent) {
                val fagsak = optionalFagsak.get()
                aksesskontroll.auditAutoriserSakstilgang(fagsak, "Søk på sak med saksnummer. Oversikt over saker og behandlinger.")
                tilFagsakOppsummeringDtoer(listOf(fagsak))
            } else {
                emptyList()
            }
        }

        StringUtils.isNotEmpty(fagsakSokDto.orgnr) -> {
            tilFagsakOppsummeringDtoer(fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, fagsakSokDto.orgnr))
        }

        else -> emptyList()
    }

    @PutMapping("/{behandlingID}/ferdigbehandle")
    @ApiOperation("Avslutt behandling med Ferdigbehandlet som resultat og oppdatere saksstatus")
    fun ferdigbehandleSak(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Void> {
        log.info("Saksbehandler {} ber om å ferdigbehandle behandling {}", SubjectHandler.getInstance().getUserID(), behandlingID)
        aksesskontroll.autoriserSkriv(behandlingID)
        ferdigbehandleService.ferdigbehandle(behandlingID)

        return ResponseEntity.noContent().build()
    }

    private fun tilFagsakDto(fagsak: Fagsak): FagsakDto = FagsakDto().apply {
        saksnummer = fagsak.saksnummer
        gsakSaksnummer = fagsak.gsakSaksnummer
        sakstema = fagsak.tema
        sakstype = fagsak.type
        saksstatus = fagsak.status
        registrertDato = fagsak.getRegistrertDato()
        endretDato = fagsak.endretDato
        hovedpartRolle = fagsak.hovedpartRolle
    }

    private fun tilFagsakOppsummeringDtoer(saker: List<Fagsak>): List<FagsakOppsummeringDto> {
        return saker.map { fagsak ->
            val fagsakBehandlinger = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()
            val saksopplysninger = hentSaksopplysninger(fagsak)

            FagsakOppsummeringDto(
                saksnummer = fagsak.saksnummer,
                sakstema = fagsak.tema,
                sakstype = fagsak.type,
                saksstatus = fagsak.status,
                opprettetDato = fagsak.getRegistrertDato(),
                hovedpartRolle = fagsak.hovedpartRolle,
                navn = hentNavn(fagsakBehandlinger),
                behandlingOversikter = fagsakBehandlinger.mapNotNull { tilBehandlingOversiktDto(it) },

                land = saksopplysninger.saksgrunnlagsbehandlingId?.let { hentLand(saksopplysninger) } ?: SoeknadslandDto(),
                periode = saksopplysninger.saksgrunnlagsbehandlingId?.let {
                    hentPeriode(saksopplysninger.sakstype, it)
                } ?: PeriodeDto()
            )
        }
    }

    private fun hentSaksopplysninger(fagsak: Fagsak): Saksopplysninger {
        val behandling = fagsak.behandlinger
            .sortedBy { it.id }
            .lastOrNull { it.type != ÅRSAVREGNING }
            ?: return Saksopplysninger(fagsak.type)

        if (!behandlingsresultatService.hentBehandlingsresultat(behandling.id).harVedtak()) {
            return Saksopplysninger(fagsak.type, behandling.id)
        }

        val sedOpplysninger = saksopplysningerService.finnSedOpplysninger(behandling.id)
            .takeIf { it.isPresent }?.get()

        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id)
            .takeIf { it.isPresent }?.get()

        return Saksopplysninger(fagsak.type, behandling.id, sedOpplysninger, mottatteOpplysninger)
    }

    private fun hentLand(saksOpplysninger: Saksopplysninger): SoeknadslandDto =
        saksOpplysninger.sedDokument?.lovvalgslandKode?.let { SoeknadslandDto.av(it) }
            ?: saksOpplysninger.motatteOpplysninger
                ?.mottatteOpplysningerData
                ?.let { MottatteOpplysningerUtils.hentLand(it) }
                ?.let { SoeknadslandDto.av(it) }
            ?: SoeknadslandDto()

    private fun hentPeriode(sakstype: Sakstyper, behandlingId: Long): PeriodeDto {
        val behandlingsResultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingId)

        if (sakstype == FTRL) {
            return PeriodeDto(
                behandlingsResultat.utledMedlemskapsperiodeFom(),
                behandlingsResultat.utledMedlemskapsperiodeTom()
            )
        }

        behandlingsResultat.finnLovvalgsperiode().getOrNull()?.let {
            return PeriodeDto(it.fom, it.tom)
        }

        return PeriodeDto()
    }

    private fun tilBehandlingOversiktDto(behandling: Behandling?): BehandlingOversiktDto? = behandling?.run {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(id)
        val tittel = "${tema.beskrivelse} - ${type.beskrivelse}" +
            if (type == ÅRSAVREGNING) " ${behandlingsresultat.årsavregning.aar}" else ""

        BehandlingOversiktDto(
            behandlingID = id,
            tittel = tittel,
            behandlingsstatus = status,
            behandlingstype = type,
            behandlingstema = tema,
            opprettetDato = getRegistrertDato(),
            behandlingsresultattype = behandlingsresultat.type,
            svarFrist = dokumentasjonSvarfristDato,
            soknadsperiode = hentSoknadsperiode(id),
        )
    }

    private fun hentSoknadsperiode(behandlingId: Long): PeriodeDto = saksopplysningerService.finnSedOpplysninger(behandlingId)
        .takeIf { it.isPresent }?.get()
        ?.let { return PeriodeDto(it.lovvalgsperiode.fom, it.lovvalgsperiode.tom) }
        ?: mottatteOpplysningerService.finnMottatteOpplysninger(behandlingId)
            .takeIf { it.isPresent }?.get()
            ?.mottatteOpplysningerData
            ?.let { MottatteOpplysningerUtils.hentPeriode(it) }
            ?.let { return PeriodeDto(it.fom, it.tom) }
        ?: PeriodeDto()

    private fun hentNavn(behandlinger: List<Behandling>): String {
        if (behandlinger.isEmpty()) return UKJENT_NAVN

        val fagsak = behandlinger[0].fagsak
        fagsak.finnBrukersAktørID()?.let { return persondataFasade.hentSammensattNavn(persondataFasade.hentFolkeregisterident(it)) }
        fagsak.finnVirksomhetsOrgnr()?.let { return organisasjonOppslagService.hentOrganisasjon(it).navn }

        return UKJENT_NAVN
    }
}
