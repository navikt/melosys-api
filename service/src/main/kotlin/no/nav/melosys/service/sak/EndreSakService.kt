package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import org.springframework.stereotype.Service

@Service
class EndreSakService(private val fagsakService: FagsakService) {
    fun endre(saksnummer: String, sakstype: Sakstyper, sakstema: Sakstemaer) {
        fagsakService.oppdaterSakstype(saksnummer, sakstype)
        fagsakService.oppdaterSakstema(saksnummer, sakstema)
    }
}
