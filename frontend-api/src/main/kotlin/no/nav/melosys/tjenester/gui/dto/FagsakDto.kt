package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import java.time.Instant

class FagsakDto {
    @JvmField
    var saksnummer: String? = null
    @JvmField
    var gsakSaksnummer: Long? = null
    @JvmField
    var sakstema: Sakstemaer? = null
    @JvmField
    var sakstype: Sakstyper? = null
    @JvmField
    var saksstatus: Saksstatuser? = null
    @JvmField
    var registrertDato: Instant? = null
    @JvmField
    var endretDato: Instant? = null
    @JvmField
    var hovedpartRolle: Aktoersroller? = null
}
