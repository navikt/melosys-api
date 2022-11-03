package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.LocalDate

class OpprettSakDto {
    var hovedpart: Aktoersroller? = null
    var brukerID: String? = null
    var virksomhetOrgnr: String? = null
    var sakstype: Sakstyper? = null
    var sakstema: Sakstemaer? = null
    var behandlingstema: Behandlingstema? = null
    var behandlingstype: Behandlingstyper? = null
    var behandlingsaarsakType: Behandlingsaarsaktyper? = null
    var behandlingsaarsakFritekst: String? = null
    var oppgaveID: String? = null
    var soknadDto: SøknadDto? = null
    var mottaksdato: LocalDate? = null
    var isSkalTilordnes = false
}
