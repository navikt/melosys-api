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
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
) {
    private val UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT =
        listOf(Saksstatuser.ANNULLERT, Saksstatuser.OPPHØRT, Saksstatuser.HENLAGT, Saksstatuser.HENLAGT_BORTFALT, Saksstatuser.VIDERESENDT)

    @Transactional(readOnly = true)
    fun harFagsakBehandlingerMedTrygdeavgift(saksnummer: String, sjekkFakturaserie: Boolean = false): Boolean =
        hentTrygdeavgiftBehandlingsresultater(saksnummer, sjekkFakturaserie).isNotEmpty()

    private fun hentTrygdeavgiftBehandlingsresultater(saksnummer: String, sjekkFakturaserie: Boolean = false): List<Behandlingsresultat> {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return emptyList()
        }

        return fagsak.behandlinger
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter {
                harTrygdeavgift(it, sjekkFakturaserie)
            }
    }

    @Transactional(readOnly = true)
    fun finnSistFakturerbarTrygdeavgiftsbehandlingForÅr(saksnummer: String, år: Int): Behandling? =
        hentTrygdeavgiftBehandlingsresultater(saksnummer).lastOrNull {
            it.medlemskapsperioder.any { it.overlapperMedÅr(år) } || it.lovvalgsperioder.any { it.overlapperMedÅr(år) }
        }?.behandling

    fun harFakturerbarTrygdeavgift(resultat: Behandlingsresultat) =
        harTrygdeavgift(resultat) && trygdeavgiftMottakerService.skalBetalesTilNav(resultat)

    private fun harTrygdeavgift(resultat: Behandlingsresultat, sjekkFakturaserie: Boolean = false) =
        harTrygdeavgift(resultat) && (!sjekkFakturaserie || harBestiltFakturaserie(resultat))

    private fun harTrygdeavgift(behandlingsresultat: Behandlingsresultat): Boolean = behandlingsresultat.trygdeavgiftsperioder.any { it.harAvgift() }

    private fun harBestiltFakturaserie(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.fakturaserieReferanse?.isNotBlank() ?: false
}
