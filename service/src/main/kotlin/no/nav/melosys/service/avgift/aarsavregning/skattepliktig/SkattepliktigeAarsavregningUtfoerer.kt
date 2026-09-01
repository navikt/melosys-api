package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.avgift.aarsavregning.SkattepliktigAarsavregningOpprettelseService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Transaksjonsskallet rundt skrivingene i skarp modus: én transaksjon per sak.
 *
 * Kjøringen går over mange saker i én løkke og fanger feil per sak for å kunne fortsette. Med
 * REQUIRED ville et kast fra en deltakende transaksjon markert den ytre rollback-only: hele batchen
 * hadde blitt rullet tilbake ved commit, mens rapporten fortsatt påsto at N prosessinstanser var
 * opprettet. Med REQUIRES_NEW mister en feilet sak bare sitt eget arbeid.
 *
 * Må ligge i egen bean — REQUIRES_NEW virker bare gjennom proxyen, ikke ved selvkall. Selve
 * arbeidet gjøres av [SkattepliktigAarsavregningOpprettelseService], som Kafka-flyten også bruker;
 * her legges bare transaksjonsgrensen på.
 */
@Component
class SkattepliktigeAarsavregningUtfoerer(
    private val opprettelseService: SkattepliktigAarsavregningOpprettelseService,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettProsessinstans(saksnummer: String, gjelderPeriode: String): UUID =
        opprettelseService.opprettProsessinstans(saksnummer, gjelderPeriode)

    /**
     * REQUIRES_NEW gir ny persistence-kontekst, så re-lesingen i
     * [SkattepliktigAarsavregningOpprettelseService.settStatusVurderDokument] treffer basen her.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun settStatusVurderDokument(
        behandlingId: Long,
        forventetStatus: Behandlingsstatus,
    ): SkattepliktigAarsavregningOpprettelseService.StatusBumpResultat =
        opprettelseService.settStatusVurderDokument(behandlingId, forventetStatus)
}
