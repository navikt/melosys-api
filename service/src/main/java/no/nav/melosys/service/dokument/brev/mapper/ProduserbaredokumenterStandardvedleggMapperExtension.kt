package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

fun Produserbaredokumenter.hentStandardvedlegg(): List<StandardvedleggType> {
    val kunInnvilgelsesstandardbrev = listOf(StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE)

    return when (this) {
        Produserbaredokumenter.TRYGDEAVTALE_AU -> kunInnvilgelsesstandardbrev
        Produserbaredokumenter.TRYGDEAVTALE_GB -> kunInnvilgelsesstandardbrev
        Produserbaredokumenter.TRYGDEAVTALE_US -> kunInnvilgelsesstandardbrev
        Produserbaredokumenter.TRYGDEAVTALE_CAN -> kunInnvilgelsesstandardbrev
        else -> listOf()
    }
}
