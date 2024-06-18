package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrygdeavgiftService(
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
) {
    private val UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT =
        listOf(Saksstatuser.ANNULLERT, Saksstatuser.OPPHØRT, Saksstatuser.HENLAGT, Saksstatuser.HENLAGT_BORTFALT, Saksstatuser.VIDERESENDT)

    @Transactional
    fun harFagsakBehandlingerMedTrygdeavgift(saksnummer: String): Boolean {
        return hentTrygdeavgiftBehandlinger(saksnummer).isNotEmpty()
    }

    @Transactional
    fun hentTrygdeavgiftBehandlinger(saksnummer: String): List<Behandling> {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return emptyList()
        }

        return fagsak
            .behandlinger
            .filter {
                harTrygdeavgift(behandlingsresultatService.hentBehandlingsresultat(it.id))
            }
    }

    fun harTrygdeavgift(behandlingsresultat: Behandlingsresultat): Boolean {
        return behandlingsresultat.trygdeavgiftsperioder.any { it.harAvgift() }
    }
}
