package no.nav.melosys.domain.mottatteopplysninger.data

import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.exception.TekniskException
import java.util.*


class Soeknadsland {
    var landkoder: List<String> = ArrayList()
    var erUkjenteEllerAlleEosLand = false

    constructor()
    constructor(landkoder: List<String>, erUkjenteEllerAlleEosLand: Boolean) {
        this.landkoder = landkoder
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand
    }

    fun erGyldig(): Boolean {
        return !landkoder.isEmpty() || erUkjenteEllerAlleEosLand
    }

    fun hentSoeknadslandForTrygdeavtale(): Trygdeavtale_myndighetsland? {
        if (landkoder.size > 1) {
            throw TekniskException("Trygdeavtale kan kun ha et søknadsland, denne har: " + landkoder.size)
        }
        return if (landkoder.isEmpty()) null else Trygdeavtale_myndighetsland.valueOf(landkoder[0])
    }

    companion object {
        fun av(vararg lovvalgsland: Land_iso2): Soeknadsland {
            return Soeknadsland(Arrays.stream(lovvalgsland).filter { obj: Land_iso2? ->
                Objects.nonNull(
                    obj
                )
            }.map { obj: Land_iso2 -> obj.kode }.toList(), false)
        }
    }
}
