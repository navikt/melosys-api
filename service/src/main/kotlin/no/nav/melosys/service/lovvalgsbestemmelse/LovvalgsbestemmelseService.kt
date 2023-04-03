package no.nav.melosys.service.lovvalgsbestemmelse

import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import org.springframework.stereotype.Service

@Service
class LovvalgsbestemmelseService {
    fun hentLovvalgsbestemmelser(
        sakstype: Sakstyper,
        sakstema: Sakstemaer?,
        behandlingstema: Behandlingstema,
        land: Land_iso2?
    ): Set<LovvalgBestemmelse> {
        return when (sakstype) {
            Sakstyper.EU_EOS -> LovvalgsbestemmelseMapperEos.mapToLovvalgsbestemmelse(behandlingstema)

            Sakstyper.TRYGDEAVTALE -> hentLovvalgsbestemmelserForTrygdeavtale(sakstema, behandlingstema, land)

            else -> throw FunksjonellException("Støtter ikke å hente lovvalgsbestemmelse for sakstype ${sakstype}")
        }
    }

    private fun hentLovvalgsbestemmelserForTrygdeavtale(
        sakstema: Sakstemaer?,
        behandlingstema: Behandlingstema,
        land: Land_iso2?
    ): Set<LovvalgBestemmelse> {
        if (sakstema == null || land == null) {
            throw FunksjonellException("Sakstema og land kan ikke være null for sakstype trygdeavtale")
        }

        return LovvalgsbestemmelseMapperAvtaleland.mapToLovvalgsbestemmelse(
            land, LovvalgsbestemmelseMappingType.utledType(sakstema, behandlingstema)
        )
    }
}
