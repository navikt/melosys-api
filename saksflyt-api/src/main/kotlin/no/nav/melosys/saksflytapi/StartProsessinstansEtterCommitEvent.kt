package no.nav.melosys.saksflytapi

import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.journalfoering.VedtakRequest
import org.springframework.context.ApplicationEvent

/**
 * Event som signaliserer at en prosessinstans skal opprettes ETTER at gjeldende transaksjon har committet.
 *
 * Dette løser et race condition-problem hvor prosesser som starter asynkront kan lese data
 * fra databasen før den opprinnelige transaksjonen har committet.
 *
 * Bruk dette eventet i stedet for å kalle prosessinstansService.opprettProsessinstans*() direkte
 * når du trenger å sikre at all data er synlig i databasen før prosessen starter.
 */
sealed class StartProsessinstansEtterCommitEvent(
    val behandlingId: Long
) : ApplicationEvent(behandlingId) {

    /**
     * Event for å starte IVERKSETT_VEDTAK_FTRL prosess etter commit.
     */
    class IverksettVedtakFtrl(
        behandlingId: Long,
        val vedtakRequest: VedtakRequest,
        val saksstatus: Saksstatuser
    ) : StartProsessinstansEtterCommitEvent(behandlingId) {
        override val prosessType: ProsessType = ProsessType.IVERKSETT_VEDTAK_FTRL
    }

    /**
     * Event for å starte IVERKSETT_VEDTAK_TRYGDEAVTALE prosess etter commit.
     */
    class IverksettVedtakTrygdeavtale(
        behandlingId: Long
    ) : StartProsessinstansEtterCommitEvent(behandlingId) {
        override val prosessType: ProsessType = ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE
    }

    /**
     * Event for å starte IVERKSETT_VEDTAK_AARSAVREGNING prosess etter commit.
     */
    class IverksettVedtakAarsavregning(
        behandlingId: Long
    ) : StartProsessinstansEtterCommitEvent(behandlingId) {
        override val prosessType: ProsessType = ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING
    }

    abstract val prosessType: ProsessType
}
