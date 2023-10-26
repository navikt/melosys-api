package no.nav.melosys.saksflytapi.journalfoering

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.LocalDate

data class OpprettSakRequest(
    var hovedpart: Aktoersroller? = null,
    var brukerID: String? = null,
    var virksomhetOrgnr: String? = null,
    var sakstype: Sakstyper? = null,
    var sakstema: Sakstemaer? = null,
    var behandlingstema: Behandlingstema? = null,
    var behandlingstype: Behandlingstyper? = null,
    var behandlingsaarsakType: Behandlingsaarsaktyper? = null,
    var behandlingsaarsakFritekst: String? = null,
    var oppgaveID: String? = null,
    var soknad: Søknad? = null,
    var mottaksdato: LocalDate? = null,
    var skalTilordnes: Boolean = false,
    var fakturaserieReferanse: String? = null,
)


class Søknad {
    var periode: Periode? = null
    var land: Soeknadsland? = null
}


