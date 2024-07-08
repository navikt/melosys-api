package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
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

        val årsavregningsår = 2024
        val fristdato = LocalDate.now().plusWeeks(4)
        val medlemskapsperiode = mapMedlemskapsPerioder(behandlingsresultat, årsavregningsår)

        return InnhentingAvInntektsopplysninger(
            brevbestilling,
            årsavregningsår,
            fristdato,
            medlemskapsperiode.first,
            medlemskapsperiode.second,
        )
    }

    private fun mapMedlemskapsPerioder(behandlingsresultat: Behandlingsresultat, årsavregningsår: Int): Pair<LocalDate, LocalDate> {


        val fom = LocalDate.now().minusWeeks(4)
        val tom = LocalDate.now().plusWeeks(4)

        return Pair(fom, tom)
    }

    private fun LocalDate.hentGyldigDatoForÅr(årsavregningsår: Int): LocalDate {
        if (year > årsavregningsår) return LocalDate.of(årsavregningsår, 12, 31)
        if (year < årsavregningsår) return LocalDate.of(årsavregningsår, 1, 1)
        return this
    }

}
