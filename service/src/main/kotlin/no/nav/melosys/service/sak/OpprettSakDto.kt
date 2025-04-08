package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.journalfoering.OpprettSakRequest
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
    //var oppgaveID: String? = null
    var soknadDto: SøknadDto? = null
    var mottaksdato: LocalDate? = null
    var skalTilordnes: Boolean = false

    fun tilOpprettSakRequest(): OpprettSakRequest {
        return OpprettSakRequest(
            hovedpart = hovedpart,
            brukerID = brukerID,
            virksomhetOrgnr = virksomhetOrgnr,
            sakstype = sakstype,
            sakstema = sakstema,
            behandlingstema = behandlingstema,
            behandlingstype = behandlingstype,
            behandlingsaarsakType = behandlingsaarsakType,
            behandlingsaarsakFritekst = behandlingsaarsakFritekst,
        //    oppgaveID = oppgaveID,
            soknad = soknadDto?.tilSøknadRequest(),
            mottaksdato = mottaksdato,
            skalTilordnes = skalTilordnes,
        )
    }
}



