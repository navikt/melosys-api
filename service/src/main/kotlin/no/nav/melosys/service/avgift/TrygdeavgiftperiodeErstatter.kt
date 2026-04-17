package no.nav.melosys.service.avgift

import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
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

        // Flush DELETEs til databasen FØR nye perioder legges til.
        // Uten dette prøver Hibernate å UPDATE (i stedet for DELETE+INSERT)
        // grunnlag-rader i samme flush, og setter inntektsperiode_id=NULL → ORA-01407.
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)

        behandlingsresultat.finnAvgiftspliktigPerioder().forEach { avgiftspliktigperiode ->
            trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
                val erLegacyMatch = trygdeavgiftsperiode.grunnlagMedlemskapsperiode?.id == avgiftspliktigperiode.hentId() ||
                    trygdeavgiftsperiode.grunnlagLovvalgsPeriode?.id == avgiftspliktigperiode.hentId() ||
                    trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode?.id == avgiftspliktigperiode.hentId()

                val erGrunnlagListeMatch = trygdeavgiftsperiode.grunnlagListe.any {
                    it.medlemskapsperiode?.hentId() == avgiftspliktigperiode.hentId() ||
                        it.lovvalgsperiode?.hentId() == avgiftspliktigperiode.hentId() ||
                        it.helseutgiftDekkesPeriode?.id == avgiftspliktigperiode.hentId()
                }

                if (erLegacyMatch || erGrunnlagListeMatch) {
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

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)

        trygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
            val matchingPeriode = finnMatchendeHelseutgiftDekkesPeriode(behandlingsresultat, trygdeavgiftsperiode, behandlingsresultatId)
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

    private fun finnMatchendeHelseutgiftDekkesPeriode(
        behandlingsresultat: Behandlingsresultat,
        trygdeavgiftsperiode: Trygdeavgiftsperiode,
        behandlingsresultatId: Long
    ) : HelseutgiftDekkesPeriode {
        val grunnlagId = trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode?.id

        if (grunnlagId != null) {
            return behandlingsresultat.helseutgiftDekkesPerioder.firstOrNull { it.id == grunnlagId }
                ?: error("Fant ingen helseutgift dekkes periode med id $grunnlagId for behandlingsresultat $behandlingsresultatId")
        }

        return behandlingsresultat.helseutgiftDekkesPerioder.singleOrNull()
            ?: error("Forventet nøyaktig én helseutgift dekkes periode for behandlingsresultat $behandlingsresultatId, men fant ${behandlingsresultat.helseutgiftDekkesPerioder.size}")
    }
}
