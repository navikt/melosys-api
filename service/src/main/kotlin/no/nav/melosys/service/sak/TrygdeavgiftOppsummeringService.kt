package no.nav.melosys.service.sak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrygdeavgiftOppsummeringService(
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
) {
    @Transactional
    fun harFagsakBehandlingerMedTrygdeavgift(saksnummer: String): Boolean {
        return hentTrygdeavgiftBehandlinger(saksnummer).isNotEmpty()
    }

    @Transactional
    fun hentTrygdeavgiftBehandlinger(saksnummer: String): List<Behandling> {
        return fagsakService.hentFagsak(saksnummer)
            .behandlinger
            .filter {
                harTrygdeavgiftOgBestiltFaktura(behandlingsresultatService.hentBehandlingsresultat(it.id))
            }
    }


    fun harTrygdeavgiftOgBestiltFaktura(behandlingsresultat: Behandlingsresultat): Boolean {
        val harTrygdeavgift = behandlingsresultat.trygdeavgiftsperioder?.any { trygdeavgiftsperiodeHarAvgift(it) } ?: false
        val bestiltFaktura = behandlingsresultat.fakturaserieReferanse?.isNotBlank() ?: false
        return harTrygdeavgift && bestiltFaktura
    }

    private fun trygdeavgiftsperiodeHarAvgift(trygdeavgiftsperiode: Trygdeavgiftsperiode?): Boolean {
        return (trygdeavgiftsperiode != null) && (trygdeavgiftsperiode.trygdeavgiftsbeløpMd != null) && trygdeavgiftsperiode.harAvgift()
    }
}
