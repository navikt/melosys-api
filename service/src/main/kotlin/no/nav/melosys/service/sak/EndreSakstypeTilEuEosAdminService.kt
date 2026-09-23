package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

/**
 * Engangsretting for MELOSYS-8309: saker fra digital søknad som har fått sakstype TRYGDEAVTALE/FTRL med et
 * behandlingstema som ikke er gyldig for sakstypen, har ingen flyt og kan ikke åpnes. Finner slike saker selv og
 * endrer sakstype til EU/EØS via [EndreSakService], slik at validering og sideeffekter blir de samme som ved
 * «Endre sak» i GUI.
 */
@Service
class EndreSakstypeTilEuEosAdminService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakService: FagsakService,
    private val endreSakService: EndreSakService,
    private val lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService
) {

    fun endreTilEuEøs(dryRun: Boolean): List<EndreSakstypeResultat> {
        val saksnumre = finnKandidater()
        log.info { "Fant ${saksnumre.size} kandidater for endring av sakstype til EU/EØS (dryRun=$dryRun)" }
        return saksnumre.mapNotNull { saksnummer ->
            try {
                endreSak(saksnummer, dryRun)
            } catch (e: Exception) {
                log.warn(e) { "Kunne ikke endre sakstype til EU/EØS for sak $saksnummer" }
                EndreSakstypeResultat(saksnummer, EndreSakstypeStatus.FEILET, e.message)
            }
        }
    }

    // Grovfilter i databasen: temaer som ikke er lovlige for sakstypen under noe sakstema.
    // Hver kandidat sjekkes deretter mot den faktiske kombinasjonen i endreSak.
    private fun finnKandidater(): List<String> =
        KILDESAKSTYPER.flatMap { sakstype ->
            val gyldigeTemaer = Sakstemaer.entries.flatMap { sakstema ->
                lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, sakstype, sakstema, null, null)
            }.toSet()
            fagsakRepository.finnDigitalSoknadSaksnumreMedAktivBehandlingUtenforTemaer(sakstype, INAKTIVE_STATUSER, gyldigeTemaer)
        }.distinct().sorted()

    private fun endreSak(saksnummer: String, dryRun: Boolean): EndreSakstypeResultat? {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.finnAktivBehandlingIkkeÅrsavregning() ?: return null
        if (kombinasjonErGyldig(fagsak, behandling)) return null

        val beskrivelse = "${fagsak.type} -> EU_EOS, behandling ${behandling.id} med tema ${behandling.tema}"
        if (dryRun) {
            return EndreSakstypeResultat(saksnummer, EndreSakstypeStatus.VIL_ENDRES, beskrivelse)
        }

        endreSakService.endre(
            saksnummer,
            Sakstyper.EU_EOS,
            fagsak.tema,
            behandling.tema,
            behandling.type,
            behandling.status,
            null
        )
        log.info { "Admin endret sakstype for sak $saksnummer: $beskrivelse" }
        return EndreSakstypeResultat(saksnummer, EndreSakstypeStatus.ENDRET, beskrivelse)
    }

    private fun kombinasjonErGyldig(fagsak: Fagsak, behandling: Behandling): Boolean = try {
        lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
            behandling, fagsak.hovedpartRolle, fagsak.type, fagsak.tema, behandling.tema, behandling.type
        )
        true
    } catch (e: FunksjonellException) {
        false
    }

    companion object {
        private val KILDESAKSTYPER = listOf(Sakstyper.TRYGDEAVTALE, Sakstyper.FTRL)
        private val INAKTIVE_STATUSER = listOf(Behandlingsstatus.AVSLUTTET, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
    }
}

data class EndreSakstypeResultat(
    val saksnummer: String,
    val status: EndreSakstypeStatus,
    val melding: String?
)

enum class EndreSakstypeStatus { VIL_ENDRES, ENDRET, FEILET }
