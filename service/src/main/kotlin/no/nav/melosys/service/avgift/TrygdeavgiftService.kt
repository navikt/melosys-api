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

    @Transactional(readOnly = true)
    fun harFagsakBehandlingerMedTrygdeavgift(saksnummer: String, sjekkFakturaserie: Boolean = false): Boolean =
        hentTrygdeavgiftBehandlinger(saksnummer, sjekkFakturaserie).isNotEmpty()

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftBehandlinger(saksnummer: String, sjekkFakturaserie: Boolean = false): List<Behandling> {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return emptyList()
        }

        return fagsak
            .behandlinger
            .filter {
                val resultat = behandlingsresultatService.hentBehandlingsresultat(it.id)
                harTrygdeavgift(resultat, sjekkFakturaserie)
            }
    }

    fun harTrygdeavgift(resultat: Behandlingsresultat, sjekkFakturaserie: Boolean = false) =
        harTrygdeavgift(resultat) && (!sjekkFakturaserie || harBestiltFakturaserie(resultat))

    private fun harTrygdeavgift(behandlingsresultat: Behandlingsresultat): Boolean = behandlingsresultat.trygdeavgiftsperioder.any { it.harAvgift() }

    private fun harBestiltFakturaserie(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.fakturaserieReferanse?.isNotBlank() ?: false
}
