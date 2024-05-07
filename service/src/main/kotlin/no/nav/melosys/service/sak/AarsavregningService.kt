package no.nav.melosys.service.sak

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service

@Service
class AarsavregningService (
    private val behandlingsresultatService: BehandlingsresultatService
) {
    fun hentEksisterendeTrygdeavgiftsperioderForFagsak(behandlingsId: Long, år: Int): List<Trygdeavgiftsperiode> =
        behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
            .medlemAvFolketrygden
            .fastsattTrygdeavgift
            .trygdeavgiftsperioder
            .filter { periode -> periode.periodeFra.year == år }
}
