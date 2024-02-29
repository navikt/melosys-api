package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper
import no.nav.melosys.domain.person.Sivilstandstype
import org.apache.commons.lang3.StringUtils


open class Sivilstand : AbstraktKodeverkHjelper() {
    override fun hentKodeverkNavn(): FellesKodeverk {
        return FellesKodeverk.SIVILSTANDER
    }

    fun tilSivilstandstypeFraDomene(): Sivilstandstype {
        val kode = getKode()
        return if (StringUtils.isEmpty(kode)) {
            Sivilstandstype.UDEFINERT
        } else when (kode) {
            "ENKE" -> Sivilstandstype.ENKE_ELLER_ENKEMANN
            "GIFT" -> Sivilstandstype.GIFT
            "GJPA" -> Sivilstandstype.GJENLEVENDE_PARTNER
            "NULL" -> Sivilstandstype.UOPPGITT
            "REPA" -> Sivilstandstype.REGISTRERT_PARTNER
            "SEPA" -> Sivilstandstype.SEPARERT_PARTNER
            "SEPR" -> Sivilstandstype.SEPARERT
            "SKIL" -> Sivilstandstype.SKILT
            "SKPA" -> Sivilstandstype.SKILT_PARTNER
            "UGI" -> Sivilstandstype.UGIFT
            else -> Sivilstandstype.UDEFINERT
        }
    }
}

