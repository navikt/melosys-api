package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import java.time.Instant

class FagsakOppsummeringDto {
    @JvmField
    var saksnummer: String? = null
    @JvmField
    var navn: String? = null
    @JvmField
    var sakstema: Sakstemaer? = null
    @JvmField
    var sakstype: Sakstyper? = null
    @JvmField
    var saksstatus: Saksstatuser? = null
    @JvmField
    var opprettetDato: Instant? = null
    var behandlingOversikter: List<BehandlingOversiktDto>? = null
    @JvmField
    var hovedpartRolle: Aktoersroller? = null
}
