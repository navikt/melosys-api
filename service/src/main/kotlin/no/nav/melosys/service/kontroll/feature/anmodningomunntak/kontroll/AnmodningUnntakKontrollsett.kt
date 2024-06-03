package no.nav.melosys.service.kontroll.feature.anmodningomunntak.kontroll

import no.nav.melosys.service.kontroll.feature.anmodningomunntak.data.AnmodningUnntakKontrollData
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.arbeidsstedLandManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.foretakUtlandManglerFelter
import no.nav.melosys.service.validering.Kontrollfeil
import java.util.function.Function

typealias Regelsett = Set<Function<AnmodningUnntakKontrollData, Kontrollfeil?>>

object AnmodningUnntakKontrollsett {
    fun hentRegler(): Regelsett {
        return REGLER_ANMODNING_UNNTAK
    }

    private val REGLER_ANMODNING_UNNTAK: Regelsett = setOf(
        Function { AnmodningUnntakKontroll.harRegistrertAdresse(it) },
        Function { AnmodningUnntakKontroll.anmodningsperiodeManglerSluttdato(it) },
        Function { AnmodningUnntakKontroll.kunEnArbeidsgiver(it) },
        Function { arbeidsstedLandManglerFelter(it.mottatteOpplysningerData) },
        Function { foretakUtlandManglerFelter(it.mottatteOpplysningerData) },
        Function { AnmodningUnntakKontroll.storbritanniaKonvensjonBruktForTidlig(it) }
    )
}
