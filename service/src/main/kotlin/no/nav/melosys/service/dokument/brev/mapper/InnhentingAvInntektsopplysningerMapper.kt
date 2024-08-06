package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
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

        val årsavregningsår = 2024 // TODO remove
        val fristdato = LocalDate.now().plusWeeks(4)
        val medlemskapsperiode = mapMedlemskapsPerioder(behandlingsresultat, årsavregningsår)

        val medlemskapsperiodeFom = medlemskapsperiode?.first
        val medlemskapsperiodeTom = medlemskapsperiode?.second

        return InnhentingAvInntektsopplysninger(
            brevbestilling,
            årsavregningsår,
            fristdato,
            medlemskapsperiodeFom,
            medlemskapsperiodeTom,
        )
    }

    private fun mapMedlemskapsPerioder(behandlingsresultat: Behandlingsresultat, årsavregningsår: Int): Pair<LocalDate, LocalDate>? {
        val relevantePerioder = hentMedlemskapsPerioderForÅrsavregning(behandlingsresultat, årsavregningsår)

        return if (relevantePerioder.isEmpty()) null
        else relevantePerioder.first().fom.tilDatoInnenforÅrsavregningsåret(årsavregningsår) to relevantePerioder.last().tom.tilDatoInnenforÅrsavregningsåret(årsavregningsår)
    }

    private fun hentMedlemskapsPerioderForÅrsavregning(behandlingsresultat: Behandlingsresultat, årsavregningsår: Int) =
        behandlingsresultat.medlemskapsperioder
            .filter { it.innvilgelsesresultat == InnvilgelsesResultat.INNVILGET }
            .filter { it.overlapperMedÅr(årsavregningsår) }
            .sortedBy { it.fom }

    private fun LocalDate.tilDatoInnenforÅrsavregningsåret(årsavregningsår: Int): LocalDate =
        when {
            this.year > årsavregningsår -> LocalDate.of(årsavregningsår, 12, 31)
            this.year < årsavregningsår -> LocalDate.of(årsavregningsår, 1, 1)
            else -> this
        }
}
