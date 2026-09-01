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
 * Klassen deltar i kallerens transaksjon og har ingen egen.
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
     *   hele kallet ryker. Uten callback propagerer kastet.
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
     * Kaster hvis saken har flere enn én — å velge en vilkårlig av dem ville gjort feil på en sak
     * ingen har sett på — og hvis en av dem er årløs, se [årFor].
     */
    fun finnAktivÅrsavregningBehandling(fagsak: Fagsak, gjelderÅr: Int): Behandling? {
        val årsavregninger = fagsak.hentAktiveÅrsavregninger()
            .filter { årFor(it, fagsak) == gjelderÅr }

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
     * Året behandlingen gjelder. Mangler behandlingen rad i `aarsavregning`, stoppes saken — å
     * opprette en ny årsavregning ved siden av den årløse ville sendt innhentingsbrev til en borger
     * på en sak ingen har sett på. Meldingen navngir behandlingen som må lukkes først.
     */
    private fun årFor(årsavregningsbehandling: Behandling, fagsak: Fagsak): Int {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(årsavregningsbehandling.id)
        // Sjekker feltet framfor å kalle hentÅrsavregning() og fange kastet derfra: den fangsten
        // ville også tatt enhver annen tilstandsfeil fra oppslaget over — en lukket EntityManager,
        // for eksempel — og sendt den som rydder til å lukke en behandling som ikke er problemet.
        return behandlingsresultat.årsavregning?.aar
            ?: throw TekniskException(
                "Aktiv ÅRSAVREGNING-behandling ${årsavregningsbehandling.id} på sak ${fagsak.saksnummer} " +
                    "mangler aarsavregning-rad (årløs). Saken stoppes i stedet for å få en ny årsavregning " +
                    "ved siden av — lukk den årløse behandlingen først, og kjør saken om igjen."
            )
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
     * Setter behandlingen til VURDER_DOKUMENT, men bare hvis den fortsatt står i [forventetStatus]
     * — statusen kalleren observerte. Uten sjekken kastes en behandling en saksbehandler har
     * flyttet videre, for eksempel til IVERKSETTER_VEDTAK, tilbake til VURDER_DOKUMENT. Å gjenta
     * inngangsbetingelsen (aktiv, ikke OPPRETTET) hjelper ikke: den er sann for nettopp de
     * statusene saksbehandleren flytter til.
     *
     * Sjekken er bare en ekte re-lesing for kallere som skriver i en egen transaksjon
     * (`REQUIRES_NEW`); leser og skriver kalleren i samme transaksjon, gir oppslaget normalt samme
     * entitet, og sammenligningen kan ikke slå ut. Den er uansett ikke atomisk — Behandling har
     * ingen `@Version`, og UPDATE-en er ikke betinget på status i SQL-en.
     *
     * Rå `status`-setting framfor `endreStatus` er bevisst: en ny skattemelding på en åpen
     * årsavregning skal ikke trigge svarfrist- eller oppgavelogikk.
     */
    fun settStatusVurderDokument(behandlingId: Long, forventetStatus: Behandlingsstatus): StatusBumpResultat {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != forventetStatus) {
            log.info {
                "Hopper over statusoppdatering for behandling $behandlingId — status er nå " +
                    "${behandling.status}, den observerte var $forventetStatus"
            }
            return StatusBumpResultat(oppdatert = false, faktiskStatus = behandling.status)
        }
        log.info { "Oppdaterer status fra ${behandling.status} til VURDER_DOKUMENT for behandling $behandlingId" }
        behandling.status = Behandlingsstatus.VURDER_DOKUMENT
        behandlingService.lagre(behandling)
        return StatusBumpResultat(oppdatert = true, faktiskStatus = Behandlingsstatus.VURDER_DOKUMENT)
    }

    /** @property faktiskStatus statusen behandlingen hadde da den ble lest på nytt. */
    data class StatusBumpResultat(val oppdatert: Boolean, val faktiskStatus: Behandlingsstatus)
}
