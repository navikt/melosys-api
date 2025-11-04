package no.nav.melosys.service.avgift

import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Component
class TrygdeavgiftperiodeErstatter(private val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattTrygdeavgiftsperioder(behandlingsresultatId: Long, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)
        nullstillTrygdeavgiftsperioder(behandlingsresultat)

        behandlingsresultat.avgiftspliktigPerioder().forEach { avgiftspliktigperiode ->
            when(avgiftspliktigperiode) {
                is Medlemskapsperiode -> trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
                    if (trygdeavgiftsperiode.grunnlagMedlemskapsperiode?.id == avgiftspliktigperiode.id) {
                        avgiftspliktigperiode.addTrygdeavgiftsperiode(trygdeavgiftsperiode)
                    }
                }
                is HelseutgiftDekkesPeriode -> trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
                    if (trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode?.id == avgiftspliktigperiode.id) {
                        avgiftspliktigperiode.addTrygdeavgiftsperiode(trygdeavgiftsperiode)
                    }
                }
                is Lovvalgsperiode -> trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
                    if (trygdeavgiftsperiode.grunnlagLovvalgsPeriode?.id == avgiftspliktigperiode.id) {
                        avgiftspliktigperiode.addTrygdeavgiftsperiode(trygdeavgiftsperiode)
                    }
                }
                else -> throw IllegalArgumentException("Ukjent avgiftspliktigperiode: ${avgiftspliktigperiode::class.java.simpleName}")
            }
        }
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattEøsPensjonistTrygdeavgiftsperioder(behandlingsresultatId: Long, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)
        nullstillEøsPensjonistTrygdeavgiftsperioder(behandlingsresultat)

        trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
            trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode = behandlingsresultat.helseutgiftDekkesPeriode
            behandlingsresultat.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        }

        val saved = behandlingsresultatService.lagre(behandlingsresultat)
        log.info("Eøs pensjonist trygdeavgiftsperioder erstattet for behandlingsresultatId: $saved")
    }

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.avgiftspliktigPerioder().forEach {
            when(it) {
                is Medlemskapsperiode -> it.clearTrygdeavgiftsperioder()
                is HelseutgiftDekkesPeriode -> it.clearTrygdeavgiftsperioder()
                is Lovvalgsperiode -> it.clearTrygdeavgiftsperioder()
            }
        }
    }

    private fun nullstillEøsPensjonistTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.hentHelseutgiftDekkesPeriode().clearTrygdeavgiftsperioder()
    }
}
