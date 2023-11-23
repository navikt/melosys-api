package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.brev.OpphoertMedlemskapBrevbestilling
import no.nav.melosys.integrasjon.dokgen.dto.OpphoertMedlemskap
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
class OpphoertMedlemskapMapper(
) {
    @Transactional
    fun map(brevbestilling: OpphoertMedlemskapBrevbestilling): OpphoertMedlemskap {
        return OpphoertMedlemskap.Builder(brevbestilling)
            // TODO: Mapping etter steget er på plass
//            .fristDato()
//            .opphoertDato()
            .build()
    }
}
