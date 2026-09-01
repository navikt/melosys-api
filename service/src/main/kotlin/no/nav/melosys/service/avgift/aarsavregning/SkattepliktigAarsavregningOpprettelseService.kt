package no.nav.melosys.service.avgift.aarsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Avgjør om en skattepliktig sak skal få en årsavregning for et gitt år, og utfører den.
 *
 * Brukes av to flyter: [SkattehendelserConsumer], som får én skattehendelse om gangen fra Kafka,
 * og kjøringsverktøyet i `skattepliktig`, som kjører de samme vurderingene i batch mot en liste
 * med hendelser. Logikken ligger her for at de to skal svare likt på samme sak.
 *
 * Klassen har ingen egen transaksjonshåndtering — den deltar i kallerens. Consumeren kjører én
 * hendelse i én transaksjon, mens verktøyet kjører hver skriving i sin egen (`REQUIRES_NEW`) for
 * at én feilet sak ikke skal rulle tilbake dem som allerede er kjørt.
 */
@Service
class SkattepliktigAarsavregningOpprettelseService(
    private val prosessinstansService: ProsessinstansService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val årsavregningService: ÅrsavregningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
) {

    /**
     * Sakene på aktøren der trygdeavgiften for [år] skal betales til Nav.
     *
     * @param onSakFeilet kalles når oppslaget på én sak kaster, og saken utelates i stedet for at
     *   hele kallet ryker. Uten callback propagerer kastet — det er consumerens feilhåndtering, der
     *   Kafka forsøker meldingen på nytt.
     */
    fun finnSakerMedTrygdeavgift(
        aktørId: String,
        år: Int,
        onSakFeilet: ((Fagsak, Exception) -> Unit)? = null,
    ): List<Fagsak> =
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .filter { fagsak ->
                if (onSakFeilet == null) {
                    skalBetalesTilNav(fagsak, år)
                } else {
                    try {
                        skalBetalesTilNav(fagsak, år)
                    } catch (e: Exception) {
                        onSakFeilet(fagsak, e)
                        false
                    }
                }
            }

    private fun skalBetalesTilNav(fagsak: Fagsak, år: Int): Boolean =
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(fagsak.saksnummer, år)
            ?.sisteBehandlingsresultatMedAvgift
            ?.let { trygdeavgiftMottakerService.skalBetalesTilNav(it) }
            ?: false

    /**
     * Den aktive årsavregningsbehandlingen for [gjelderÅr], eller null hvis saken ikke har noen.
     *
     * Kaster hvis saken har flere enn én: å bumpe en vilkårlig av dem og ignorere resten ville
     * gjort feil på en sak ingen har sett på.
     */
    fun finnAktivÅrsavregningBehandling(fagsak: Fagsak, gjelderÅr: Int): Behandling? {
        val årsavregninger = fagsak.hentAktiveÅrsavregninger()
            .filter { behandlingsresultatService.hentBehandlingsresultat(it.id).hentÅrsavregning().aar == gjelderÅr }

        return when {
            årsavregninger.isEmpty() -> {
                log.info("Fant ingen aktive årsavregninger for år $gjelderÅr")
                null
            }

            årsavregninger.size > 1 ->
                throw TekniskException("Flere aktive årsavregninger funnet for sak: ${fagsak.saksnummer} og år: $gjelderÅr")

            else -> {
                log.info("Fant aktiv årsavregning for ${fagsak.saksnummer} og år $gjelderÅr")
                årsavregninger.single()
            }
        }
    }

    /**
     * `sendInnhentingsbrev = true`: saken skal ha brevet «Innhenting av inntektsopplysninger».
     */
    fun opprettProsessinstans(saksnummer: String, gjelderPeriode: String): UUID =
        prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
            saksnummer,
            gjelderPeriode,
            Behandlingsaarsaktyper.MELDING_FRA_SKATT,
            true,
        )

    /**
     * Rå `status`-setting framfor `endreStatus` er bevisst: en ny skattemelding på en åpen
     * årsavregning skal ikke trigge svarfrist- eller oppgavelogikk.
     */
    fun settStatusVurderDokument(behandling: Behandling) {
        log.info { "Oppdaterer status fra ${behandling.status} til VURDER_DOKUMENT for behandling ${behandling.id}" }
        behandling.status = Behandlingsstatus.VURDER_DOKUMENT
        behandlingService.lagre(behandling)
    }
}
