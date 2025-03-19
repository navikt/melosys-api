package no.nav.melosys.tjenester.gui.fagsaker

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
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
import no.nav.melosys.tjenester.gui.dto.BehandlingOversiktDto
import no.nav.melosys.tjenester.gui.dto.FagsakDto
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto
import no.nav.melosys.tjenester.gui.dto.FagsakSokDto
import no.nav.melosys.tjenester.gui.dto.SoeknadslandDto.Companion.av
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import no.nav.security.token.support.core.api.Protected
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext
import java.util.function.Consumer

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
    @GetMapping("/{saksnr}")
    @ApiOperation(
        value = "Henter en sak med et gitt saksnummer",
        notes = ("Spesifikke saker kan hentes via saksnummer.")
    )
    fun hentFagsak(@PathVariable("saksnr") saksnummer: String?): ResponseEntity<FagsakDto?> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        aksesskontroll.autoriserSakstilgang(fagsak)
        val fagsakDto = tilFagsakDto(fagsak)
        log.info("Henting av sak {} ({})", fagsakDto.saksnummer, fagsakDto.gsakSaksnummer)
        return ResponseEntity.ok(fagsakDto)
    }

    @PostMapping
    @ApiOperation(value = "Oppretter en ny sak.")
    fun opprettNySak(@RequestBody opprettSakDto: OpprettSakDto): ResponseEntity<Void?> {
        if (opprettSakDto.brukerID == null && opprettSakDto.virksomhetOrgnr == null) {
            throw FunksjonellException("BrukerID eller organisasjonsnummer trengs for å opprette en sak.")
        }
        if (opprettSakDto.brukerID != null) {
            aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.brukerID)
        }

        if (opprettSakDto.oppgaveID == null) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        } else {
            opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto)
        }

        return ResponseEntity.noContent().build<Void?>()
    }

    @PostMapping("/{saksnr}/behandlinger")
    @ApiOperation(value = "Oppretter en ny behandling for sak.")
    fun opprettNyBehandlingForSak(
        @PathVariable("saksnr") saksnummer: String?,
        @RequestBody opprettSakDto: OpprettSakDto
    ): ResponseEntity<Void?> {
        if (opprettSakDto.brukerID != null) {
            aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.brukerID)
        }

        opprettBehandlingForSak.opprettBehandling(saksnummer, opprettSakDto)

        return ResponseEntity.noContent().build<Void?>()
    }

    @PutMapping("/{saksnr}")
    @ApiOperation(value = "Endre en sak.")
    fun endreFagsak(
        @PathVariable("saksnr") saksnummer: String,
        @RequestBody endreDto: EndreSakDto
    ): ResponseEntity<Void?> {
        log.debug(
            "Saksbehandler {} ber om å endre fagsak {} med sakstype {}, sakstema {}",
            SubjectHandler.getInstance().getUserID(), saksnummer, endreDto.sakstype, endreDto.sakstema
        )
        aksesskontroll.autoriserSakstilgang(saksnummer)

        if (endreDto.behandlingstype == Behandlingstyper.ÅRSAVREGNING && endreDto.behandlingID != null) {
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

        return ResponseEntity.noContent().build<Void?>()
    }

    @PostMapping("/sok")
    @ApiOperation(
        value = "Søk etter saker på ident eller saksnummer eller orgnr",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer. Saker knyttet til en organisasjon søkes via organisasjonsnummer."),
        response = FagsakOppsummeringDto::class,
        responseContainer = "List"
    )
    fun hentFagsaker(@RequestBody fagsakSokDto: FagsakSokDto): MutableList<FagsakOppsummeringDto?> {
        if (StringUtils.isNotEmpty(fagsakSokDto.ident)) {
            aksesskontroll.auditAutoriserFolkeregisterIdent(
                fagsakSokDto.ident,
                "Søk på person med ident. Oversikt over saker og behandlinger."
            )
            return tilFagsakOppsummeringDtoer(
                fagsakService.hentFagsakerMedAktør(
                    Aktoersroller.BRUKER,
                    fagsakSokDto.ident
                )
            )
        } else if (StringUtils.isNotEmpty(fagsakSokDto.saksnummer)) {
            val fagsak = fagsakService.finnFagsakFraSaksnummer(fagsakSokDto.saksnummer)
            if (fagsak.isPresent) {
                aksesskontroll.auditAutoriserSakstilgang(
                    fagsak.get(),
                    "Søk på sak med saksnummer. Oversikt over saker og behandlinger."
                )
                return tilFagsakOppsummeringDtoer(listOf<Fagsak>(fagsak.get()))
            }
        } else if (StringUtils.isNotEmpty(fagsakSokDto.orgnr)) {
            return tilFagsakOppsummeringDtoer(
                fagsakService.hentFagsakerMedOrgnr(
                    Aktoersroller.VIRKSOMHET,
                    fagsakSokDto.orgnr
                )
            )
        }

        return mutableListOf<FagsakOppsummeringDto?>()
    }

    @PutMapping("/{behandlingID}/ferdigbehandle")
    @ApiOperation("Avslutt behandling med Ferdigbehandlet som resultat og oppdatere saksstatus")
    fun ferdigbehandleSak(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Void?> {
        log.info(
            "Saksbehandler {} ber om å ferdigbehandle behandling {}",
            SubjectHandler.getInstance().getUserID(),
            behandlingID
        )
        aksesskontroll.autoriserSkriv(behandlingID)

        ferdigbehandleService.ferdigbehandle(behandlingID)

        return ResponseEntity.noContent().build<Void?>()
    }


    private fun tilFagsakDto(fagsak: Fagsak): FagsakDto {
        val fagsakDto = FagsakDto()
        fagsakDto.saksnummer = fagsak.saksnummer
        fagsakDto.gsakSaksnummer = fagsak.gsakSaksnummer
        fagsakDto.sakstema = fagsak.tema
        fagsakDto.sakstype = fagsak.type
        fagsakDto.saksstatus = fagsak.status
        fagsakDto.registrertDato = fagsak.getRegistrertDato()
        fagsakDto.endretDato = fagsak.endretDato
        fagsakDto.hovedpartRolle = fagsak.hovedpartRolle

        return fagsakDto
    }

    private fun tilFagsakOppsummeringDtoer(saker: List<Fagsak>): MutableList<FagsakOppsummeringDto?> {
        val fagsakListe: MutableList<FagsakOppsummeringDto?> = ArrayList<FagsakOppsummeringDto?>()
        for (fagsak in saker) {
            val fagsakOppsummeringDto = FagsakOppsummeringDto()
            fagsakOppsummeringDto.saksnummer = fagsak.saksnummer
            fagsakOppsummeringDto.sakstema = fagsak.tema
            fagsakOppsummeringDto.sakstype = fagsak.type
            fagsakOppsummeringDto.saksstatus = fagsak.status
            fagsakOppsummeringDto.opprettetDato = fagsak.getRegistrertDato()
            fagsakOppsummeringDto.hovedpartRolle = fagsak.hovedpartRolle

            val behandlinger = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()

            val behandlingOversiktDtoer = behandlinger.map { behandling -> tilBehandlingOversiktDto(behandling) }

            fagsakOppsummeringDto.navn = hentNavn(behandlinger)
            fagsakOppsummeringDto.behandlingOversikter = behandlingOversiktDtoer
            fagsakListe.add(fagsakOppsummeringDto)
        }
        return fagsakListe
    }

    private fun tilBehandlingOversiktDto(behandling: Behandling?): BehandlingOversiktDto {
        val behandlingOversiktDto = BehandlingOversiktDto()
        if (behandling != null) {
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

            behandlingOversiktDto.behandlingID = behandling.id
            behandlingOversiktDto.behandlingsstatus = behandling.status
            behandlingOversiktDto.behandlingstype = behandling.type
            behandlingOversiktDto.behandlingstema = behandling.tema
            behandlingOversiktDto.opprettetDato = behandling.getRegistrertDato()
            behandlingOversiktDto.behandlingsresultattype = behandlingsresultat.type
            behandlingOversiktDto.svarFrist = behandling.dokumentasjonSvarfristDato

            setPeriodeOpplysninger(behandling, behandlingOversiktDto)
        }
        return behandlingOversiktDto
    }

    private fun setPeriodeOpplysninger(behandling: Behandling, behandlingOversiktDto: BehandlingOversiktDto) {
        saksopplysningerService.finnSedOpplysninger(behandling.id).let { sedOptional ->
            if (sedOptional.isPresent) {
                sedOptional.get().let { sed ->
                    behandlingOversiktDto.apply {
                        land = av(sed.lovvalgslandKode)
                        soknadsperiode = PeriodeDto(sed.lovvalgsperiode.fom, sed.lovvalgsperiode.tom)
                    }
                }
            } else {
                mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).ifPresent { mottatteOpplysninger ->
                    mottatteOpplysninger.mottatteOpplysningerData.let { data ->
                        behandlingOversiktDto.apply {
                            land = av(MottatteOpplysningerUtils.hentLand(data))
                            MottatteOpplysningerUtils.hentPeriode(data)?.let { periode ->
                                soknadsperiode = PeriodeDto(periode.fom, periode.tom)
                            }
                        }
                    }
                }
            }
        }


        val behandlingsResultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandling.id)

        behandlingsResultat.finnLovvalgsperiode().ifPresent(Consumer { lovvalgsperiode: Lovvalgsperiode? ->
            val periode = PeriodeDto(lovvalgsperiode!!.fom, lovvalgsperiode.tom)
            behandlingOversiktDto.lovvalgsperiode = periode
        })

        val periode = PeriodeDto(
            behandlingsResultat.utledMedlemskapsperiodeFom(),
            behandlingsResultat.utledMedlemskapsperiodeTom()
        )
        behandlingOversiktDto.medlemskapsperiode = periode
    }

    private fun hentNavn(behandlinger: List<Behandling>): String? {
        if (behandlinger.isEmpty()) {
            return UKJENT_NAVN
        }
        val fagsak = behandlinger.get(0)!!.fagsak
        val aktørId = fagsak.finnBrukersAktørID()
        if (aktørId != null) {
            return persondataFasade.hentSammensattNavn(persondataFasade.hentFolkeregisterident(aktørId))
        }
        val orgnr = fagsak.finnVirksomhetsOrgnr()
        if (orgnr != null) {
            return organisasjonOppslagService.hentOrganisasjon(orgnr).navn
        }
        return UKJENT_NAVN
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FagsakController::class.java)
        private const val UKJENT_NAVN = "UKJENT"
    }
}
