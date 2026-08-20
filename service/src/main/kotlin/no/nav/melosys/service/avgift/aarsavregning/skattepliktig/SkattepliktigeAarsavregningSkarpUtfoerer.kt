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
     * Behandlingen hentes på nytt her: entiteten kalleren sitter på er lastet i den ytre,
     * read-only persistence-konteksten og er detached for denne transaksjonen.
     *
     * Rå `status`-setting + `lagre` (ikke `endreStatus`) er bevisst — det speiler
     * SkattehendelserConsumer, som heller ikke skal trigge svarfrist- eller oppgave-logikk.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun settStatusVurderDokument(behandlingId: Long) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        log.info { "SKARP: oppdaterer status fra ${behandling.status} til VURDER_DOKUMENT for behandling $behandlingId" }
        behandling.status = Behandlingsstatus.VURDER_DOKUMENT
        behandlingService.lagre(behandling)
    }
}
