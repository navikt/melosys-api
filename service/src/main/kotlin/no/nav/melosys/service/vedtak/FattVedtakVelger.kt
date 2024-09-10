package no.nav.melosys.service.vedtak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.springframework.stereotype.Component

@Component
class FattVedtakVelger(
    private val eosVedtakService: EosVedtakService,
    private val ftrlVedtakService: FtrlVedtakService,
    private val trygdeavtaleVedtakService: TrygdeavtaleVedtakService,
    private val årsavregningVedtakService: ÅrsavregningVedtakService
) {
    fun getFattVedtakService(behandling: Behandling): FattVedtakInterface {
        if (behandling.type == Behandlingstyper.ÅRSAVREGNING) {
            return årsavregningVedtakService
        }

        return when (behandling.fagsak.type) {
            Sakstyper.EU_EOS -> eosVedtakService
            Sakstyper.FTRL -> ftrlVedtakService
            Sakstyper.TRYGDEAVTALE -> trygdeavtaleVedtakService
        }
    }
}
