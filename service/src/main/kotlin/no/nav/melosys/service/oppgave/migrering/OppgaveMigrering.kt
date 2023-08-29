package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.repository.BehandlingRepositoryForOppgaveMigrering
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
import no.nav.melosys.service.oppgave.*
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OppgaveMigrering(
    private val behandlingRepository: BehandlingRepositoryForOppgaveMigrering,
    private val oppgaveFasade: OppgaveFasade,
    private val migreringsRapport: MigreringsRapport,
    private val prosessinstansRepository: ProsessinstansRepository
) {
    @Volatile
    var stopMigrering: Boolean = false

    private val oppgaveBehandlingstemaUtleder = OppgaveBehandlingstemaUtleder(OppgaveGosysMappingMedRegler())
    private val oppgavetypeUtleder = OppgavetypeUtleder(OppgaveGosysMappingMedRegler())
    private val oppgaveBeskrivelseUtleder = OppgaveBeskrivelseUtleder(OppgaveGosysMappingMedRegler())
    private val temaUtleder = OppgaveTemaUtleder()

    fun status(): Map<String, Any> {
        return migreringsRapport.status()
    }

    private val behandlingsstatuser = listOf(
        Behandlingsstatus.AVSLUTTET,
        Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING,
        Behandlingsstatus.IVERKSETTER_VEDTAK,
    )

    @Async
    @Synchronized
    fun go(bruker: String?, saksnummer: String?, dryrun: Boolean, options: Options = Options()) {
        ThreadLocalAccessInfo.executeProcess("Migrer oppgaver") {
            migrering(bruker, saksnummer, dryrun, options)
        }
    }

    fun stop() {
        stopMigrering = true
        log.info("Migrering stoppet!")
    }

    private fun finnSaker(
        bruker: String?,
        saksnummer: String?
    ): MutableCollection<SakOgBehandlingDTO> {
        if (saksnummer != null) {
            return behandlingRepository.finnSak(saksnummer, behandlingsstatuser)
        }
        if (bruker != null) {
            return behandlingRepository.finnSakerRegistrertAv(bruker, behandlingsstatuser)
        }
        return behandlingRepository.finnSaksOgBehandlingTyperOgTema(behandlingsstatuser)
    }

    internal fun migrering(
        bruker: String?,
        saksnummer: String?,
        dryrun: Boolean,
        options: Options = Options()
    ) {
        log.info(
            "Utfører OppgaveMigrering. \n" +
                options.toJsonNode.toPrettyString()
        )
        finnSaker(bruker, saksnummer).apply {
            migreringsRapport.antallSakerFunnet = size
        }.filter { it.erRedigerbar() }.sortedBy { it.saksnummer }.apply {
            migreringsRapport.antallSakerErRedigerbar = size
        }.asSequence().filter {
            options.saksFilter.filtrer(it)
        }.filter { !stopMigrering }.map {
            MigreringsSak(
                sak = it,
                oppgaver = oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(it.saksnummer),
                ny = nyOppgaveMapping(it)
            )
        }.filter {
            erGyldig(it.sak)
        }.onEach {
            migreringsRapport.antallSakerProssessert++
            leggTilRapport(it)
        }.filterNot {
            it.ny.fantIkkeOppgaveMapping()
        }.forEach {
            if (it.oppgaver.isEmpty() && options.lagManglendeOppgaver) {
                lagOppgave(dryrun, it)
            } else if(options.migrerOppgaver) {
                oppdaterOppgave(dryrun, it)
            }
        }
        log.info("OppgaveMigrering utført! ${if (stopMigrering) "Stoppet manuelt!" else ""}")
        lagRapport()
    }

    private fun erGyldig(sak: SakOgBehandlingDTO): Boolean =
        erGyldig(sak.sakstype, sak.sakstema, sak.behandlingstype, sak.behandlingstema).apply {
            if (!this) {
                migreringsRapport.harIkkeGyldigKombinasjon++
                log.warn(
                    "${sak.saksnummer} har ikke gyldig kombinasjon:" +
                        " sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype} "
                )
            }
        }

    private fun leggTilRapport(migreringsSak: MigreringsSak) {
        val oppgaver = migreringsSak.oppgaver
        val sak = migreringsSak.sak
        migreringsRapport.migrertSak(migreringsSak)
        if (oppgaver.isEmpty()) {
            migreringsRapport.sakManglerOppgave(sak.saksnummer)
        }
        if (oppgaver.size > 1) {
            migreringsRapport.sakerMedFlereOppgaver("fant ${oppgaver.size} oppgaver for: ${sak.saksnummer}")
        }
    }

    private fun lagOppgave(dryrun: Boolean, migreringsSak: MigreringsSak) {
        if (dryrun) {
            migreringsRapport.antallOppgaverLaget++
            return
        }

        val sak = migreringsSak.sak
        val oppdatering = migreringsSak.ny
        val oppgave = Oppgave.Builder()
            .setOppgavetype(oppdatering.oppgaveType)
            .setBehandlingstema(oppdatering.oppgaveBehandlingstema?.kode)
            .setTema(oppdatering.tema)
            .setBeskrivelse("Gjennopprettet oppgave til behandling i Melosys - <${sak.saksnummer}>")
            .build()
        try {
            oppgaveFasade.opprettOppgave(oppgave)
            migreringsRapport.antallOppgaverLaget++
        } catch (e: Exception) {
            migreringsSak.ny.oppgaveOppdateringError = e.message
            log.error("lagOppgave feilet for ${sak.saksnummer}(${sak.behandlingID})", e)
            migreringsRapport.migreringFeilet++
            var sleepTime = 100L * migreringsRapport.migreringFeilet
            if (sleepTime > 1000) sleepTime = 1000
            Thread.sleep(sleepTime)
        }
    }

    private fun oppdaterOppgave(dryrun: Boolean, migreringsSak: MigreringsSak) {
        if (migreringsSak.erOppgaveMigrert()) {
            migreringsRapport.alleredeMigrert++
            return
        }
        if (dryrun) {
            migreringsRapport.antallSakerMigrert++ // Så vi ser hvor mange vi kommer til å migrere
            return
        }

        val sak = migreringsSak.sak
        val oppgaveId = migreringsSak.oppgaver.first().oppgaveId
        val oppdatering = migreringsSak.ny

        val oppgaveOppdatering = OppgaveOppdatering
            .builder()
            .oppgavetype(oppdatering.oppgaveType)
            .behandlingstema(oppdatering.oppgaveBehandlingstema?.kode)
            .behandlingstype("") // Må være tom streng og ikke null slik at feltet slettes i oppgave
            .tema(oppdatering.tema)
            .beskrivelse("") // Vi vil ikke oppdatere beskrivelsen for oppggave i migreringen
            .build()
        try {
            oppgaveFasade.oppdaterOppgave(oppgaveId, oppgaveOppdatering)
            migreringsRapport.antallSakerMigrert++
        } catch (e: Exception) {
            migreringsSak.ny.oppgaveOppdateringError = e.message
            log.error("oppdaterOppgave feilet for ${sak.saksnummer}(${sak.behandlingID}) oppgaveID:$oppgaveId", e)
            migreringsRapport.migreringFeilet++
            var sleepTime = 100L * migreringsRapport.migreringFeilet
            if (sleepTime > 1000) sleepTime = 1000
            Thread.sleep(sleepTime)
        }
    }

    private fun nyOppgaveMapping(sakOgBehandling: SakOgBehandlingDTO): OppgaveMigreringsOppdatering {
        try {
            val oppgaveBehandlingstema: OppgaveBehandlingstema? = sakOgBehandling.utledOppgaveBehandlingstema()
            val oppgavetype: Oppgavetyper = sakOgBehandling.utledOppgaveType()
            val beskrivelse: String = sakOgBehandling.utledBeskrivelse()
            val tema: Tema = sakOgBehandling.utledTema()
            return OppgaveMigreringsOppdatering(oppgaveBehandlingstema, null, tema, oppgavetype, beskrivelse)
        } catch (e: Exception) {
            val gyldige = GyldigeKombinasjoner.finnGyldige(
                sakOgBehandling.sakstype,
                sakOgBehandling.sakstema,
                sakOgBehandling.behandlingstype,
                sakOgBehandling.behandlingstema
            )
            val mappingError = "${sakOgBehandling.saksnummer}: gyldige:${gyldige.size}  ${e.message}"
            migreringsRapport.mappingFeiler(mappingError)
            return OppgaveMigreringsOppdatering(mappingError)
        }
    }

    private fun finnSEDKobletTilBehandling(it: SakOgBehandlingDTO): MutableList<String> {
        if (!relevanteBehandlinger(it)) {
            return mutableListOf()
        }

        val prosessinstanserMedBehandling = prosessinstansRepository.findAllByBehandling_Id(it.behandlingID)
        val prosessinstanserMedSedLåsReferanse = prosessinstanserMedBehandling.map { it.låsReferanse }.filterNotNull()
            .map { prosessinstansRepository.findAllByLåsReferanseStartingWith(it) }.flatten()

        return (prosessinstanserMedBehandling + prosessinstanserMedSedLåsReferanse)
            .asSequence()
            .sortedBy { it.registrertDato }
            .map { it.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java, null) }
            .filterNotNull()
            .distinctBy { it.sedId }
            .map { it.sedType }
            .toMutableList()
    }

    private fun relevanteBehandlinger(it: SakOgBehandlingDTO): Boolean {
        return it.sakstype == Sakstyper.EU_EOS
            && it.sakstema == Sakstemaer.UNNTAK
            && listOf(
            Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
        ).contains(it.behandlingstema)
            && it.behandlingstype == Behandlingstyper.FØRSTEGANG
            && it.behandlingstatus == Behandlingsstatus.VURDER_DOKUMENT
    }

    private fun SakOgBehandlingDTO.utledOppgaveBehandlingstema(): OppgaveBehandlingstema? =
        oppgaveBehandlingstemaUtleder.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    private fun SakOgBehandlingDTO.utledOppgaveType(): Oppgavetyper =
        oppgavetypeUtleder.utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype)

    private fun SakOgBehandlingDTO.utledTema(): Tema =
        temaUtleder.utledTema(
            sakstype, sakstema, behandlingstema
        )

    private fun SakOgBehandlingDTO.utledBeskrivelse(): String {
        val hentSedDokument = { _: Boolean -> sedDokument(behandlingID) }
        return try {
            oppgaveBeskrivelseUtleder.utledBeskrivelse(
                sakstype,
                sakstema,
                behandlingstema,
                behandlingstype,
                hentSedDokument
            )
        } catch (e: Exception) {
            val message = e.message ?: "utledBeskrivelse feilet "
            val msg = "$message feilet for $saksnummer, behandlingID:$behandlingID"
            log.warn(msg)
            migreringsRapport.finnesIkkeSedForSak(msg)
            return msg
        }
    }

    private fun sedDokument(behandlingID: Long): SedDokument? =
        hentBehandlingMedSaksoplysninger(behandlingID)
            .finnSedDokument().orElse(null)

    private fun hentBehandlingMedSaksoplysninger(behandlingID: Long): Behandling = behandlingRepository
        .findWithSaksopplysningerById(behandlingID) ?: throw TekniskException("Fant ikke behandling for $behandlingID")

    private fun lagRapport() {
        val status = migreringsRapport.statusEtterKjøring()
        log.info(status)
        migreringsRapport.saveStatusFiles(status)
    }

    companion object {
        private val oppgaveGosysMapping by lazy { OppgaveGosysMappingMedRegler() }
        internal fun erGyldig(
            sakstype: Sakstyper,
            sakstema: Sakstemaer,
            behandlingstype: Behandlingstyper,
            behandlingstema: Behandlingstema,
        ): Boolean = finnGyldige(sakstype, sakstema, behandlingstype, behandlingstema).isNotEmpty()

        private fun finnGyldige(
            sakstype: Sakstyper,
            sakstema: Sakstemaer,
            behandlingstype: Behandlingstyper,
            behandlingstema: Behandlingstema,
        ) = GyldigeKombinasjoner.finnGyldige(
            sakstype, sakstema, behandlingstype, behandlingstema
        ) + unntakForMigrering.filter {
            it.match(sakstype, sakstema, behandlingstype, behandlingstema)
        }

        private val unntakForMigrering = listOf(
            GyldigeKombinasjoner.TableRow(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstype = Behandlingstyper.FØRSTEGANG,
                behandlingstema = Behandlingstema.TRYGDETID
            ), GyldigeKombinasjoner.TableRow(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstype = Behandlingstyper.NY_VURDERING,
                behandlingstema = Behandlingstema.TRYGDETID,
            )
        )

        internal fun finnOppgave(
            sakstype: Sakstyper,
            sakstema: Sakstemaer,
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper?
        ): OppgaveGosysMapping.Oppgave {
            return oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype)
        }
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }

    internal class OppgaveGosysMappingMedRegler : OppgaveGosysMapping() {
        override fun finnOppgave(
            sakstype: Sakstyper,
            sakstema: Sakstemaer,
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper?
        ): Oppgave {
            return super.finnOppgaveOrNull(sakstype, sakstema, behandlingstema, behandlingstype)
                ?: listOf(
                    TableRow(
                        Sakstyper.EU_EOS,
                        Sakstemaer.MEDLEMSKAP_LOVVALG,
                        setOf(
                            Behandlingstyper.FØRSTEGANG,
                            Behandlingstyper.NY_VURDERING
                        ),
                        setOf(Behandlingstema.TRYGDETID),
                        Oppgave(
                            OppgaveBehandlingstema.EU_EOS_FORESPORSEL_OM_TRYGDETID,
                            Tema.MED,
                            Oppgavetyper.BEH_SED,
                            Beskrivelsefelt.SED,
                            Regel.KUN_VED_MIGRERING
                        )
                    )
                ).find {
                    it.sakstype == sakstype && it.sakstema == sakstema && behandlingstype in it.behandlingstype && behandlingstema in it.behandlingstema
                }?.oppgave ?: throw IllegalStateException(
                    "Fant ikke oppgave mapping for " +
                        "sakstype:$sakstype, sakstema:$sakstema, behandlingstema:$behandlingstema, behandlingstype:$behandlingstype"
                )
        }
    }

    data class Options(
        val migrerOppgaver: Boolean = true,
        val lagManglendeOppgaver: Boolean = false,
        val saksFilter: SaksFilter = SaksFilter()
    )

    data class SaksFilter(
        val sakstyper: List<Sakstyper> = listOf(),
        val sakstemar: List<Sakstemaer> = listOf(),
        val behandlingstyper: List<Behandlingstyper> = listOf(),
        val behandlingstemaer: List<Behandlingstema> = listOf()
    ) {
        fun filtrer(sak: SakOgBehandlingDTO): Boolean = (sakstyper.isEmpty() || sak.sakstype in sakstyper) &&
            (sakstemar.isEmpty() || sak.sakstema in sakstemar) &&
            (behandlingstemaer.isEmpty() || sak.behandlingstema in behandlingstemaer) &&
            (behandlingstyper.isEmpty() || sak.behandlingstype in behandlingstyper)

    }
}
