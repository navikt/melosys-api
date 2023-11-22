package no.nav.melosys.service.sak

import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TrygdeavgiftService(
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
) {
    @Transactional
    fun harFagsakBehandlingerMedTrygdeavgift(saksnummer: String): Boolean {
        return fagsakService.hentFagsak(saksnummer)
            .behandlinger
            .map { behandling -> behandlingsresultatService.hentBehandlingsresultat(behandling.id) }
            .any {
                it.medlemAvFolketrygden?.fastsattTrygdeavgift?.trygdeavgiftsperioder?.filter { it.trygdeavgiftsbeløpMd != null && it.trygdeavgiftsbeløpMd?.verdi != BigDecimal.ZERO }
                    ?.isNotEmpty() ?: false
            }
    }
}
