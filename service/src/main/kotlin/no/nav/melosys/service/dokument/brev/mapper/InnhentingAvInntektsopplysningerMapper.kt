package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.InnhentingAvInntektsopplysninger
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class InnhentingAvInntektsopplysningerMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
) {
    @Transactional
    internal fun map(brevbestilling: InnhentingAvInntektsopplysningerBrevbestilling): InnhentingAvInntektsopplysninger {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)

        if(behandlingsresultat.årsavregning == null){
            throw FunksjonellException("Årsavregningsår er ikke valgt")
        }

        val årsavregningsår = behandlingsresultat.hentÅrsavregning().aar

        val fristdato = LocalDate.now().plusWeeks(4)
        val avgiftspliktigPeriode = hentFørsteOgSisteAvgiftspliktigPeriode(behandlingsresultat, årsavregningsår)

        val medlemskapsperiodeFom = avgiftspliktigPeriode?.first
        val medlemskapsperiodeTom = avgiftspliktigPeriode?.second

        return InnhentingAvInntektsopplysninger(
            brevbestilling,
            årsavregningsår,
            fristdato,
            medlemskapsperiodeFom,
            medlemskapsperiodeTom,
        )
    }

    private fun hentFørsteOgSisteAvgiftspliktigPeriode(behandlingsresultat: Behandlingsresultat, årsavregningsår: Int): Pair<LocalDate, LocalDate>? {
        val relevantePerioder = hentAvgiftspliktigPerioderForÅrsavregning(behandlingsresultat, årsavregningsår)

        if (relevantePerioder.isEmpty()) return null

        val fom = checkNotNull(relevantePerioder.first().getFom()) { "fom er påkrevd for avgiftspliktig periode" }
        val tom = checkNotNull(relevantePerioder.last().getTom()) { "tom er påkrevd for avgiftspliktig periode" }
        return fom.tilDatoInnenforÅrsavregningsåret(årsavregningsår) to tom.tilDatoInnenforÅrsavregningsåret(årsavregningsår)
    }

    private fun hentAvgiftspliktigPerioderForÅrsavregning(behandlingsresultat: Behandlingsresultat, årsavregningsår: Int) =
        behandlingsresultat.finnAvgiftspliktigPerioder()
            .filter { it.erInnvilget() }
            .filter { it.overlapperMedÅr(årsavregningsår) }
            .sortedBy { it.getFom() }

    private fun LocalDate.tilDatoInnenforÅrsavregningsåret(årsavregningsår: Int): LocalDate =
        when {
            this.year > årsavregningsår -> LocalDate.of(årsavregningsår, 12, 31)
            this.year < årsavregningsår -> LocalDate.of(årsavregningsår, 1, 1)
            else -> this
        }
}
