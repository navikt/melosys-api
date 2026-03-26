package no.nav.melosys.service.avgift

import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
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

        behandlingsresultat.finnAvgiftspliktigPerioder().forEach { avgiftspliktigperiode ->
            trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
                val erMatch = trygdeavgiftsperiode.grunnlagMedlemskapsperiode?.id == avgiftspliktigperiode.hentId() ||
                    trygdeavgiftsperiode.grunnlagLovvalgsPeriode?.id == avgiftspliktigperiode.hentId() ||
                    trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode?.id == avgiftspliktigperiode.hentId()

                if (erMatch) {
                    avgiftspliktigperiode.addTrygdeavgiftsperiode(trygdeavgiftsperiode)
                }
            }
        }
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattEøsPensjonistTrygdeavgiftsperioder(behandlingsresultatId: Long, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)
        nullstillEøsPensjonistTrygdeavgiftsperioder(behandlingsresultat)

        trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
            val grunnlagId = trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode?.id
            val matchingPeriode = if (grunnlagId != null) {
                behandlingsresultat.helseutgiftDekkesPerioder.firstOrNull { it.id == grunnlagId }
            } else {
                null
            }
                ?: behandlingsresultat.helseutgiftDekkesPerioder.singleOrNull()
                ?: error("Ingen matchende helseutgift dekkes periode funnet for behandlingsresultat $behandlingsresultatId")
            trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode = matchingPeriode
            matchingPeriode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        }

        val saved = behandlingsresultatService.lagre(behandlingsresultat)
        log.info("Eøs pensjonist trygdeavgiftsperioder erstattet for behandlingsresultatId: $saved")
    }

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.finnAvgiftspliktigPerioder().forEach {
            it.clearTrygdeavgiftsperioder()
        }
    }

    private fun nullstillEøsPensjonistTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.helseutgiftDekkesPerioder.forEach { it.clearTrygdeavgiftsperioder() }
    }
}
