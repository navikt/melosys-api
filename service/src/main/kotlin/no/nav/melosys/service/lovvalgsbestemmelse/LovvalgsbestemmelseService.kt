package no.nav.melosys.service.lovvalgsbestemmelse

import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.springframework.stereotype.Service

@Service
class LovvalgsbestemmelseService {
    fun hentLovvalgsbestemmelser(
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        land: Land_iso2
    ): Set<LovvalgBestemmelse> {
        return LovvalgsbestemmelseMapper.mapToLovvalgsbestemmelse(land, LovvalgsbestemmelseMappingType.utledType(sakstema, behandlingstema))
    }
}
