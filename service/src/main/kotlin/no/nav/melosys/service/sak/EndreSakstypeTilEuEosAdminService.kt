package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

/**
 * Engangsretting for MELOSYS-8309: saker som har fått sakstype TRYGDEAVTALE/FTRL med et behandlingstema
 * som bare finnes for EU/EØS, har ingen flyt og kan ikke åpnes. Endrer sakstype til EU/EØS via
 * [EndreSakService], slik at validering og sideeffekter blir de samme som ved «Endre sak» i GUI.
 * Saker med gyldig kombinasjon for sakstypen røres ikke.
 */
@Service
class EndreSakstypeTilEuEosAdminService(
    private val fagsakService: FagsakService,
    private val endreSakService: EndreSakService,
    private val lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService
) {

    fun endreTilEuEøs(saksnumre: List<String>, dryRun: Boolean): List<EndreSakstypeResultat> =
        saksnumre.distinct().map { saksnummer ->
            try {
                endreSak(saksnummer, dryRun)
            } catch (e: Exception) {
                log.warn(e) { "Kunne ikke endre sakstype til EU/EØS for sak $saksnummer" }
                EndreSakstypeResultat(saksnummer, EndreSakstypeStatus.FEILET, e.message)
            }
        }

    private fun endreSak(saksnummer: String, dryRun: Boolean): EndreSakstypeResultat {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        if (fagsak.type == Sakstyper.EU_EOS) {
            return EndreSakstypeResultat(saksnummer, EndreSakstypeStatus.HOPPET_OVER, "Saken er allerede EU/EØS")
        }
        val behandling = fagsak.finnAktivBehandlingIkkeÅrsavregning()
            ?: return EndreSakstypeResultat(saksnummer, EndreSakstypeStatus.HOPPET_OVER, "Saken har ingen aktiv behandling")

        if (kombinasjonErGyldig(fagsak.hovedpartRolle, fagsak.type, fagsak.tema, behandling)) {
            return EndreSakstypeResultat(
                saksnummer,
                EndreSakstypeStatus.HOPPET_OVER,
                "Behandlingstema ${behandling.tema} er gyldig for sakstype ${fagsak.type}"
            )
        }

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

    private fun kombinasjonErGyldig(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandling: Behandling
    ): Boolean = try {
        lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
            behandling, hovedpart, sakstype, sakstema, behandling.tema, behandling.type
        )
        true
    } catch (e: FunksjonellException) {
        false
    }
}

data class EndreSakstypeResultat(
    val saksnummer: String,
    val status: EndreSakstypeStatus,
    val melding: String?
)

enum class EndreSakstypeStatus { VIL_ENDRES, ENDRET, HOPPET_OVER, FEILET }
