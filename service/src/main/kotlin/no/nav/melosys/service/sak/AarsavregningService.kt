package no.nav.melosys.service.sak

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service

@Service
class AarsavregningService (
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService
) {
    //TODO Ikke egentlig en del av 6570, men er en forutsetning for å lage datagrunnlaget for 6570
    fun hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer: String, år: Int): List<Trygdeavgiftsperiode> {
        return fagsakService.hentFagsak(saksnummer)
            .behandlinger
            .flatMap { behandling ->
                behandlingsresultatService.hentBehandlingsresultat(behandling.id)
                    .medlemAvFolketrygden
                    .fastsattTrygdeavgift
                    .trygdeavgiftsperioder
                    .filter { periode -> periode.periodeFra.year == år }
            }
    }
}
