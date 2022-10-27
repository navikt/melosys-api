package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

internal class OpprettSakDto {
    var hovedpart: Aktoersroller? = null
    var brukerID: String? = null
    var virksomhetOrgnr: String? = null
    var sakstype: Sakstyper? = null
    var sakstema: Sakstemaer? = null
    var behandlingstema: Behandlingstema? = null
    var behandlingstype: Behandlingstyper? = null
    var oppgaveID: String? = null
    var soknadDto: SøknadDto? = null

    var isSkalTilordnes = false
}
