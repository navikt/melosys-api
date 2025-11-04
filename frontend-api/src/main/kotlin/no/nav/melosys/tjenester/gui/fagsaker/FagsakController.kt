package no.nav.melosys.tjenester.gui.fagsaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Sakstemaer.*
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ÅRSAVREGNING
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.util.MottatteOpplysningerUtils
import io.getunleash.Unleash
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
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
@Tag(name = "fagsaker")
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
    private val ferdigbehandleService: FerdigbehandleService,
    private val unleash: Unleash,
) {
    private val log = KotlinLogging.logger { }
    private val UKJENT_NAVN = "UKJENT"

    @GetMapping("/{saksnr}")
    @Operation(
        summary = "Henter en sak med et gitt saksnummer",
        description = ("Spesifikke saker kan hentes via saksnummer.")
    )
    fun hentFagsak(@PathVariable("saksnr") saksnummer: String): ResponseEntity<FagsakDto> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        aksesskontroll.autoriserSakstilgang(fagsak)
        val fagsakDto = tilFagsakDto(fagsak)
        log.info("Henting av sak {} ({})", fagsakDto.saksnummer, fagsakDto.gsakSaksnummer)
        return ResponseEntity.ok(fagsakDto)
    }

    @PostMapping
    @Operation(summary = "Oppretter en ny sak.")
    fun opprettNySak(@RequestBody opprettSakDto: OpprettSakDto): ResponseEntity<Void> {
        if (opprettSakDto.brukerID == null && opprettSakDto.virksomhetOrgnr == null) {
            throw FunksjonellException("BrukerID eller organisasjonsnummer trengs for å opprette en sak.")
        }

        opprettSakDto.brukerID?.let { aksesskontroll.autoriserFolkeregisterIdent(it) }

        opprettSak.opprettNySakOgBehandling(opprettSakDto)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{saksnr}/behandlinger")
    @Operation(summary = "Oppretter en ny behandling for sak.")
    fun opprettNyBehandlingForSak(
        @PathVariable("saksnr") saksnummer: String?,
        @RequestBody opprettSakDto: OpprettSakDto
    ): ResponseEntity<Void> {
        opprettSakDto.brukerID?.let { aksesskontroll.autoriserFolkeregisterIdent(it) }
        opprettBehandlingForSak.opprettBehandling(saksnummer, opprettSakDto)

        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{saksnr}")
    @Operation(summary = "Endre en sak.")
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
    @Operation(
        summary = "Søk etter saker på ident eller saksnummer eller orgnr",
        description = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer. Saker knyttet til en organisasjon søkes via organisasjonsnummer.")
    )
    fun hentFagsaker(
        @RequestBody fagsakSokDto: FagsakSokDto,
        @RequestParam(defaultValue = "true") aktiveBehandlinger: Boolean
    ): List<FagsakOppsummeringDto> = when {
        StringUtils.isNotEmpty(fagsakSokDto.ident) -> {
            aksesskontroll.auditAutoriserFolkeregisterIdent(
                fagsakSokDto.ident, "Søk på person med ident. Oversikt over saker og behandlinger."
            )
            tilFagsakOppsummeringDtoer(
                fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, fagsakSokDto.ident),
                aktiveBehandlinger
            )
        }

        StringUtils.isNotEmpty(fagsakSokDto.saksnummer) -> {
            val optionalFagsak = fagsakService.finnFagsakFraSaksnummer(fagsakSokDto.saksnummer)
            if (optionalFagsak.isPresent) {
                val fagsak = optionalFagsak.get()
                aksesskontroll.auditAutoriserSakstilgang(fagsak, "Søk på sak med saksnummer. Oversikt over saker og behandlinger.")
                tilFagsakOppsummeringDtoer(
                    listOf(fagsak),
                    aktiveBehandlinger
                )
            } else {
                emptyList()
            }
        }

        StringUtils.isNotEmpty(fagsakSokDto.orgnr) -> {
            tilFagsakOppsummeringDtoer(
                fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, fagsakSokDto.orgnr),
                aktiveBehandlinger
            )
        }

        else -> emptyList()
    }

    @PutMapping("/{behandlingID}/ferdigbehandle")
    @Operation(summary = "Avslutt behandling med Ferdigbehandlet som resultat og oppdatere saksstatus")
    fun ferdigbehandleSak(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Void> {
        log.info("Saksbehandler {} ber om å ferdigbehandle behandling {}", SubjectHandler.getInstance().getUserID(), behandlingID)
        aksesskontroll.autoriserSkriv(behandlingID)
        ferdigbehandleService.ferdigbehandle(behandlingID)

        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{saksnr}/betalingsvalg")
    @Operation(summary = "Lagrer betalingsvalg som saksopplysning for pensjonister")
    fun lagreBetalingsvalgSomSaksopplysning(
        @PathVariable("saksnr") saksnummer: String,
        @RequestBody betalingstype: Betalingstype
    ): ResponseEntity<Void> {
        log.debug(
            "Saksbehandler {} ber om å lagre betalingsvalg {} som saksopplysning",
            SubjectHandler.getInstance().getUserID(), betalingstype
        )
        aksesskontroll.autoriserSakstilgang(saksnummer)
        fagsakService.lagreBetalingsvalg(saksnummer, betalingstype)

        return ResponseEntity.noContent().build()
    }

    private fun tilFagsakDto(fagsak: Fagsak): FagsakDto = FagsakDto().apply {
        saksnummer = fagsak.saksnummer
        gsakSaksnummer = fagsak.gsakSaksnummer
        sakstema = fagsak.tema
        sakstype = fagsak.type
        saksstatus = fagsak.status
        betalingsvalg = fagsak.betalingsvalg
        registrertDato = fagsak.getRegistrertDato()
        endretDato = fagsak.endretDato
        hovedpartRolle = fagsak.hovedpartRolle
    }


    private fun tilFagsakOppsummeringDtoer(saker: List<Fagsak>, aktiveBehandlinger: Boolean): List<FagsakOppsummeringDto> {
        val sorterteFagsaker = if (unleash.isEnabled(ToggleName.MELOSYS_SORTER_SOK_PA_REDIGERINGSDATO)) {
            saker.sortedByDescending { it.endretDato }
        } else {
            saker
        }

        return sorterteFagsaker.map { fagsak ->
            val saksopplysninger = hentSaksopplysninger(fagsak, aktiveBehandlinger)

            val sorterteFagsakBehandlinger = if (unleash.isEnabled(ToggleName.MELOSYS_SORTER_SOK_PA_REDIGERINGSDATO)) {
                fagsak.behandlinger.sortedByDescending { it.endretDato }
            } else {
                fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()
            }

            FagsakOppsummeringDto(
                saksnummer = fagsak.saksnummer,
                sakstema = fagsak.tema,
                sakstype = fagsak.type,
                saksstatus = fagsak.status,
                opprettetDato = fagsak.getRegistrertDato(),
                hovedpartRolle = fagsak.hovedpartRolle,
                navn = hentNavn(sorterteFagsakBehandlinger),
                behandlingOversikter = sorterteFagsakBehandlinger.mapNotNull { tilBehandlingOversiktDto(it) },
                land = saksopplysninger.saksgrunnlagsbehandlingId?.let { hentLand(saksopplysninger, fagsak) } ?: SoeknadslandDto(),
                periode = saksopplysninger.saksgrunnlagsbehandlingId?.let { hentPeriode(saksopplysninger, fagsak, it) } ?: PeriodeDto()
            )
        }
    }

    private fun hentSistHelseutgiftDekkesPeriode(fagsak: Fagsak): HelseutgiftDekkesPeriode? =
        fagsak.hentSistEndretBehandlingIkkeÅrsavregning()?.let {
            behandlingsresultatService.hentBehandlingsresultat(it.id).helseutgiftDekkesPeriode
        }

    private fun hentSaksopplysninger(fagsak: Fagsak, aktiveBehandlinger: Boolean): Saksopplysninger {
        val behandling = hentSisteBehandlingMedFattetVedtakIkkeÅrsavregning(fagsak)
            ?: (if (fagsak.tema == UNNTAK) finnSistAvsluttetBehandlingMedLovvalgsperiodeIkkeÅrsavregning(fagsak) else null)
            ?: (if (aktiveBehandlinger) fagsak.finnAktivBehandlingIkkeÅrsavregning() else null)
            ?: return Saksopplysninger(fagsak.type)

        val sedOpplysninger = saksopplysningerService.finnSedOpplysninger(behandling.id)
            .takeIf { it.isPresent }?.get()

        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id)
            .takeIf { it.isPresent }?.get()

        return Saksopplysninger(fagsak.type, behandling.id, sedOpplysninger, mottatteOpplysninger)
    }

    private fun hentSisteBehandlingMedFattetVedtakIkkeÅrsavregning(fagsak: Fagsak): Behandling? =
        fagsak.behandlinger
            .filter { it.type != ÅRSAVREGNING }
            .filter { behandlingsresultatService.hentBehandlingsresultat(it.id).harVedtak() }
            .maxByOrNull { it.endretDato }


    fun finnSistAvsluttetBehandlingMedLovvalgsperiodeIkkeÅrsavregning(fagsak: Fagsak): Behandling? =
        fagsak.behandlinger
            .filter { it.erAvsluttet() && !it.erÅrsavregning() && !it.erHenvendelse() }
            .filter { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(it.id).finnLovvalgsperiode().isPresent }
            .maxByOrNull { it.endretDato }


    private fun hentLand(saksOpplysninger: Saksopplysninger, fagsak: Fagsak): SoeknadslandDto {
        val sakstema = fagsak.tema

        val soeknadslandDto = when (sakstema) {
            MEDLEMSKAP_LOVVALG -> {
                saksOpplysninger.mottatteOpplysninger?.mottatteOpplysningerData?.let { mottatteOpplysningerData ->
                    return SoeknadslandDto(MottatteOpplysningerUtils.hentLand(mottatteOpplysningerData).landkoder)
                }
            }

            UNNTAK -> {
                saksOpplysninger.sedDokument?.let { sedDokument ->
                    return SoeknadslandDto(listOf(sedDokument.hentAvsenderLandkode().kode))
                }

                saksOpplysninger.mottatteOpplysninger?.mottatteOpplysningerData?.let { mottatteOpplysningerData ->
                    return SoeknadslandDto(MottatteOpplysningerUtils.hentLand(mottatteOpplysningerData).landkoder)
                }
            }

            TRYGDEAVGIFT ->
                saksOpplysninger.mottatteOpplysninger?.behandling
                    ?.takeIf { it.erEøsPensjonist() }
                    ?.let {
                        hentSistHelseutgiftDekkesPeriode(fagsak)?.let { helseutgiftDekkesPeriode ->
                            return SoeknadslandDto(listOf(helseutgiftDekkesPeriode.bostedLandkode.kode))
                        }
                    }
        }

        return soeknadslandDto ?: SoeknadslandDto()
    }

    private fun hentPeriode(saksOpplysninger: Saksopplysninger, fagsak: Fagsak, behandlingId: Long): PeriodeDto {
        val behandlingsresultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingId)
        val sakstype = saksOpplysninger.sakstype
        val sakstema = fagsak.tema

        val periode = when (sakstype) {
            FTRL -> {
                val medlemskapsperioder = behandlingsresultat.medlemskapsperioder

                if (medlemskapsperioder.isEmpty()) return hentSoknadsperiode(behandlingId)

                return PeriodeDto(
                    behandlingsresultat.utledMedlemskapsperiodeFom(),
                    behandlingsresultat.utledMedlemskapsperiodeTom()
                )
            }

            TRYGDEAVTALE, EU_EOS -> {
                if (sakstema == TRYGDEAVGIFT) {
                    saksOpplysninger.mottatteOpplysninger?.behandling?.let { behandling ->
                        if (behandling.erEøsPensjonist()) {
                            val helseutgiftDekkesPeriode = hentSistHelseutgiftDekkesPeriode(fagsak) ?: return PeriodeDto()

                            return PeriodeDto(
                                helseutgiftDekkesPeriode.fomDato,
                                helseutgiftDekkesPeriode.tomDato
                            )
                        }
                    }
                }

                behandlingsresultat.finnLovvalgsperiode().getOrNull()?.let {
                    return PeriodeDto(it.fom, it.tom)
                }
            }
        }

        return periode ?: hentSoknadsperiode(behandlingId)
    }

    private fun tilBehandlingOversiktDto(behandling: Behandling?): BehandlingOversiktDto? = behandling?.run {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(id)
        val tittel = buildString {
            append("${type.beskrivelse}")
            if (type == ÅRSAVREGNING) {
                behandlingsresultat.årsavregning?.aar?.let { append(" $it") }
            }
        }

        BehandlingOversiktDto(
            behandlingID = id,
            tittel = tittel,
            behandlingsstatus = status,
            behandlingstype = type,
            behandlingstema = tema,
            opprettetDato = getRegistrertDato(),
            behandlingsresultattype = behandlingsresultat.hentType(),
            svarFrist = dokumentasjonSvarfristDato,
            soknadsperiode = hentSoknadsperiode(id),
        )
    }

    private fun hentSoknadsperiode(behandlingId: Long): PeriodeDto = saksopplysningerService.finnSedOpplysninger(behandlingId)
        .takeIf { it.isPresent }?.get()
        ?.let { return PeriodeDto(it.hentLovvalgsperiode().fom, it.hentLovvalgsperiode().tom) }
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

data class Saksopplysninger(
    val sakstype: Sakstyper,
    val saksgrunnlagsbehandlingId: Long? = null,
    val sedDokument: SedDokument? = null,
    val mottatteOpplysninger: MottatteOpplysninger? = null,
)
