package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Side-effektene i skarp modus, én transaksjon per sak.
 *
 * Kjøringen går over mange saker i én løkke og fanger feil per sak for å kunne fortsette.
 * Med REQUIRED ville et kast fra en deltakende transaksjon markert den ytre transaksjonen
 * rollback-only: hele batchen hadde blitt rullet tilbake ved commit, mens rapporten fortsatt
 * påsto at N prosessinstanser var opprettet. Med REQUIRES_NEW mister en feilet sak bare sitt
 * eget arbeid, og det som allerede er committet overlever.
 *
 * Må ligge i egen bean — REQUIRES_NEW virker bare gjennom proxyen, ikke ved selvkall.
 */
@Component
class SkattepliktigeAarsavregningSkarpUtfoerer(
    private val prosessinstansService: ProsessinstansService,
    private val behandlingService: BehandlingService,
) {

    /**
     * `sendInnhentingsbrev = true` speiler [no.nav.melosys.service.avgift.aarsavregning.SkattehendelserConsumer],
     * som denne kjøringen er en manuell replay av: sakene skal ha brevet
     * «Innhenting av inntektsopplysninger» på lik linje med dem som kom via Kafka.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettProsessinstans(saksnummer: String, gjelderPeriode: String): UUID =
        prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
            saksnummer,
            gjelderPeriode,
            Behandlingsaarsaktyper.MELDING_FRA_SKATT,
            true,
        )

    /**
     * Bumper kun hvis den ferske raden fortsatt står i [forventetStatus] — statusen løkka faktisk
     * observerte da den bestemte seg for å bumpe.
     *
     * Merk at dette er en read-check-write innenfor én transaksjon, ikke en atomisk compare-and-set:
     * Behandling har ingen @Version, og UPDATE-en er ikke betinget på status i SQL-en. Et vindu på
     * lengden av denne transaksjonen består derfor. Det er samme vindu som SkattehendelserConsumer
     * har i dag, og fire-fem størrelsesordener mindre enn det løkka hadde uten sjekken (der vinduet
     * var hele batchen). Ekte atomisitet krever @Version på Behandling eller en betinget UPDATE som
     * omgår auditing — begge er større grep enn denne jobben.
     *
     * SkattehendelserConsumer holder sjekk og skriving i én kort transaksjon, men har det samme vinduet
     * — én transaksjon er ikke atomisk uten lås, versjonskolonne eller betinget UPDATE. Forskjellen er
     * lengden: der er vinduet én sakslesing, her ble de to skilt av REQUIRES_NEW, og med 45 saker og
     * tunge oppslag er vinduet minutter. Uten CAS-en ville en behandling en saksbehandler flyttet videre i mellomtiden — f.eks.
     * til IVERKSETTER_VEDTAK — blitt kastet tilbake til VURDER_DOKUMENT. Å gjenta løkkas
     * inngangsbetingelse (aktiv, ikke OPPRETTET) er ikke nok: den er sann for nettopp de statusene
     * saksbehandleren flytter til.
     *
     * CAS-en subsumerer aktiv-sjekken: både AVSLUTTET og MIDLERTIDIG_LOVVALGSBESLUTNING er statuser,
     * så en drift dit gir uansett mismatch.
     *
     * Behandlingen hentes på nytt fordi entiteten kalleren sitter på er lastet i den ytre, read-only
     * persistence-konteksten og er detached for denne transaksjonen.
     *
     * Rå `status`-setting + `lagre` (ikke `endreStatus`) er bevisst — det speiler
     * SkattehendelserConsumer, som heller ikke skal trigge svarfrist- eller oppgave-logikk.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun settStatusVurderDokument(behandlingId: Long, forventetStatus: Behandlingsstatus): StatusBumpResultat {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != forventetStatus) {
            log.info {
                "SKARP: hopper over status-bump for behandling $behandlingId — status er nå " +
                    "${behandling.status}, løkka observerte $forventetStatus"
            }
            return StatusBumpResultat(oppdatert = false, faktiskStatus = behandling.status)
        }
        log.info { "SKARP: oppdaterer status fra ${behandling.status} til VURDER_DOKUMENT for behandling $behandlingId" }
        behandling.status = Behandlingsstatus.VURDER_DOKUMENT
        behandlingService.lagre(behandling)
        return StatusBumpResultat(oppdatert = true, faktiskStatus = Behandlingsstatus.VURDER_DOKUMENT)
    }

    /** @property faktiskStatus statusen raden hadde da den indre transaksjonen leste den. */
    data class StatusBumpResultat(val oppdatert: Boolean, val faktiskStatus: Behandlingsstatus)
}
