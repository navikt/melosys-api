package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.service.brev.BrevAdresse

class MottakerDto {
    var type: String? = null
    var rolle: Mottakerroller? = null
    var orgnrSettesAvSaksbehandler: Boolean = false
    var adresser: Collection<BrevAdresse>? = null
    var feilmelding: FeilmeldingDto? = null
}
