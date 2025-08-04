package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.FagsakService.UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrygdeavgiftService(
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
) {

    @Transactional(readOnly = true)
    fun harFagsakBehandlingerMedTrygdeavgift(saksnummer: String, sjekkFakturaserie: Boolean = false): Boolean {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return false
        }

        return fagsak.behandlinger.any {
            harFakturerbarTrygdeavgift(behandlingsresultatService.hentBehandlingsresultat(it.id), sjekkFakturaserie)
        }
    }

    @Transactional
    fun slettTrygdeavgiftsperioderPåBehandlingsresultat(behandlingID: Long) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        behandlingsresultat.clearTrygdeavgiftsperioder()
    }

    fun harFakturerbarTrygdeavgift(resultat: Behandlingsresultat, sjekkFakturaserie: Boolean = false) =
        harTrygdeavgift(resultat, sjekkFakturaserie) && trygdeavgiftMottakerService.skalBetalesTilNav(resultat)

    private fun harTrygdeavgift(resultat: Behandlingsresultat, sjekkFakturaserie: Boolean = false) =
        harTrygdeavgift(resultat) && (!sjekkFakturaserie || harBestiltFakturaserie(resultat))

    private fun harTrygdeavgift(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.trygdeavgiftsperioder.any { it.harAvgift() } || behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.any { it.harAvgift() }

    private fun harBestiltFakturaserie(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.fakturaserieReferanse?.isNotBlank() ?: false
}
