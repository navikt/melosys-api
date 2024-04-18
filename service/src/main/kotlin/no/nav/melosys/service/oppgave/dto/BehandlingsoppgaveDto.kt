package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.service.felles.dto.SoeknadslandDto

class BehandlingsoppgaveDto : OppgaveDto() {
    @JvmField
    var behandling: BehandlingDto = BehandlingDto()
    @JvmField
    var land: SoeknadslandDto? = null
    var saksnummer: String? = null
    var sakstype: Sakstyper? = null
    var sakstema: Sakstemaer? = null
    var periode: PeriodeDto = PeriodeDto()
    var oppgaveBeskrivelse: String? = null
    @JvmField
    var sisteNotat: String? = null
}
