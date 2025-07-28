package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class TrygdeavgiftperiodeErstatter(private val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattTrygdeavgiftsperioder(behandlingsresultatId: Long, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)
        nullstillTrygdeavgiftsperioder(behandlingsresultat)

        behandlingsresultat.medlemskapsperioder.forEach { medlemskapsperiode ->
            trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
                if (trygdeavgiftsperiode.grunnlagMedlemskapsperiode?.id == medlemskapsperiode.id) {
                    medlemskapsperiode.addTrygdeavgiftsperiode(trygdeavgiftsperiode)
                }
            }
        }
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattEøsPensjonistTrygdeavgiftsperioder(behandlingsresultatId: Long, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)
        nullstillEøsPensjonistTrygdeavgiftsperioder(behandlingsresultat)
        behandlingsresultat.helseutgiftDekkesPeriode.trygdeavgiftsperioder = trygdeavgiftsperioder.toMutableSet()

        behandlingsresultatService.lagre(behandlingsresultat)
    }

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.clearTrygdeavgiftsperioder()
        }
    }

    private fun nullstillEøsPensjonistTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.helseutgiftDekkesPeriode.clearTrygdeavgiftsperioder()
    }
}
