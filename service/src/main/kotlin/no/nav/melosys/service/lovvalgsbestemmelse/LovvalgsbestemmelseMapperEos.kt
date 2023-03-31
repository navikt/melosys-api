package no.nav.melosys.service.lovvalgsbestemmelse

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException

class LovvalgsbestemmelseMapperEos {
    companion object {

        fun mapToLovvalgsbestemmelse(behandlingstema: Behandlingstema): Set<LovvalgBestemmelse> {
            return when (behandlingstema) {
                Behandlingstema.IKKE_YRKESAKTIV -> setOf(
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2,
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E,
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
                )

                else -> throw FunksjonellException("Støtter ikke henting av lovvalgsbestemmelser for behandlingstema ${behandlingstema}")
            }
        }
    }
}
