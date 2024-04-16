package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData
import no.nav.melosys.service.validering.Kontrollfeil
import java.util.function.Function

object FerdigbehandlingKontrollsett {
    fun hentRegelsettForVedtak(
        sakstype: Sakstyper,
        harRegistreringUnntakFraMedlemskapFlyt: Boolean,
        harIkkeYrkesaktivFlyt: Boolean
    ): Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> {
        if (harRegistreringUnntakFraMedlemskapFlyt) {
            return REGELSETT_UNNTAKSREGISTRERING
        }
        if (harIkkeYrkesaktivFlyt) {
            return REGELSETT_IKKE_YRKESAKTIV
        }

        return when (sakstype) {
            Sakstyper.EU_EOS -> REGELSETT_EU_EOS
            Sakstyper.FTRL -> REGELSETT_FTRL
            Sakstyper.TRYGDEAVTALE -> REGELSETT_TRYGDEAVTALER
        }
    }

    fun hentRegelsettForAvslagOgHenleggelse(): Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> {
        return REGELSETT_AVSLAG_HENLEGGELSE
    }

    private val REGELSETT_EU_EOS: Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> = setOf(
        Function { FerdigbehandlingKontroll.adresseRegistrert(it) },
        Function { FerdigbehandlingKontroll.overlappendePeriode(it) },
        Function { FerdigbehandlingKontroll.periodeOver24Mnd(it) },
        Function { FerdigbehandlingKontroll.periodeManglerSluttdato(it) },
        Function { FerdigbehandlingKontroll.arbeidsstedLandManglerFelter(it) },
        Function { FerdigbehandlingKontroll.arbeidsstedMaritimtManglerFelter(it) },
        Function { FerdigbehandlingKontroll.arbeidsstedOffshoreManglerFelter(it) },
        Function { FerdigbehandlingKontroll.arbeidsstedLuftfartManglerFelter(it) },
        Function { FerdigbehandlingKontroll.foretakUtlandManglerFelter(it) },
        Function { FerdigbehandlingKontroll.selvstendigUtlandManglerFelter(it) },
        Function { FerdigbehandlingKontroll.orgnrErOpphørt(it) },
        Function { FerdigbehandlingKontroll.åpentUtkastFinnes(it) })

    private val REGELSETT_FTRL: Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> = setOf(
        Function { FerdigbehandlingKontroll.adresseRegistrert(it) },
        Function { FerdigbehandlingKontroll.overlappendePeriode(it) },
        Function { FerdigbehandlingKontroll.åpentUtkastFinnes(it) })

    private val REGELSETT_TRYGDEAVTALER: Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> = setOf(
        Function { FerdigbehandlingKontroll.adresseRegistrert(it) },
        Function { FerdigbehandlingKontroll.overlappendePeriode(it) },
        Function { FerdigbehandlingKontroll.periodeOver12Måneder(it) },
        Function { FerdigbehandlingKontroll.periodeOverTreÅr(it) },
        Function { FerdigbehandlingKontroll.periodeOverFemÅr(it) },
        Function { FerdigbehandlingKontroll.periodeManglerSluttdato(it) },
        Function { FerdigbehandlingKontroll.arbeidsstedLandManglerFelter(it) },
        Function { FerdigbehandlingKontroll.representantIUtlandetMangler(it) },
        Function { FerdigbehandlingKontroll.åpentUtkastFinnes(it) })

    private val REGELSETT_UNNTAKSREGISTRERING: Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> = setOf(
        Function { FerdigbehandlingKontroll.overlappendeMedlemskapsperiode(it) },
        Function { FerdigbehandlingKontroll.periodeManglerSluttdato(it) },
        Function { FerdigbehandlingKontroll.overlappendeUnntaksperiode(it) },
        Function { FerdigbehandlingKontroll.åpentUtkastFinnes(it) })

    private val REGELSETT_IKKE_YRKESAKTIV: Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> = setOf(
        Function { FerdigbehandlingKontroll.adresseRegistrert(it) },
        Function { FerdigbehandlingKontroll.overlappendePeriode(it) },
        Function { FerdigbehandlingKontroll.periodeManglerSluttdato(it) },
        Function { FerdigbehandlingKontroll.åpentUtkastFinnes(it) })

    private val REGELSETT_AVSLAG_HENLEGGELSE: Set<Function<FerdigbehandlingKontrollData, Kontrollfeil?>> = setOf(
        Function { FerdigbehandlingKontroll.adresseRegistrert(it) },
        Function { FerdigbehandlingKontroll.åpentUtkastFinnes(it) })
}
