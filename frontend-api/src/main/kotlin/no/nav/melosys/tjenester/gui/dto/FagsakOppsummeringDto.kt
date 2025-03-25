package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import java.time.Instant

class FagsakOppsummeringDto {
    var saksnummer: String? = null
    var navn: String? = null
    var sakstema: Sakstemaer? = null
    var sakstype: Sakstyper? = null
    var saksstatus: Saksstatuser? = null
    var opprettetDato: Instant? = null
    var behandlingOversikter: List<BehandlingOversiktDto?>? = null
    var hovedpartRolle: Aktoersroller? = null
}
