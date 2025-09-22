package no.nav.melosys.domain.eessi.sed

import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import org.apache.commons.lang3.StringUtils
import java.util.*

class Virksomhet {
    var navn: String? = null
    var adresse: Adresse? = null
    var orgnr: String? = null

    constructor()

    constructor(navn: String?, orgnr: String?, adresse: Adresse?) {
        this.navn = navn
        this.orgnr = if (StringUtils.isBlank(orgnr)) UKJENT else orgnr
        this.adresse = adresse
    }

    fun tilForetakUtland(): ForetakUtland = tilForetakUtland(false)

    fun tilSelvstendigForetakUtland(): ForetakUtland = tilForetakUtland(true)

    private fun tilForetakUtland(erSelvstendig: Boolean): ForetakUtland = ForetakUtland().apply {
        uuid = UUID.randomUUID().toString()
        navn = this@Virksomhet.navn
        orgnr = this@Virksomhet.orgnr
        this@Virksomhet.adresse?.let {
            adresse = it.tilStrukturertAdresse()
        }
        selvstendigNæringsvirksomhet = erSelvstendig
    }

    fun hentOrgnrEllerNavn(): String? =
        if (StringUtils.isNotEmpty(orgnr)) orgnr else navn

    companion object {
        private const val UKJENT = "Unknown"
    }
}
