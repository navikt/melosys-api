package no.nav.melosys.service.lovvalgsbestemmelse

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004.*
import no.nav.melosys.exception.FunksjonellException

class LovvalgsbestemmelseMapperEos {
    companion object {

        fun mapToLovvalgsbestemmelse(behandlingstema: Behandlingstema): Set<LovvalgBestemmelse> {
            return when (behandlingstema) {
                Behandlingstema.IKKE_YRKESAKTIV -> setOf(
                    FO_883_2004_ART11_2,
                    FO_883_2004_ART11_3E,
                    FO_883_2004_ART16_1
                )

                Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR -> mutableSetOf<LovvalgBestemmelse>(
                    *Lovvalgbestemmelser_883_2004.values(),
                    *Lovvalgbestemmelser_987_2009.values(),
                    *Tilleggsbestemmelser_883_2004.values(),
                    *Overgangsregelbestemmelser.values()
                ).filterNot { lovvalgBestemmelse ->
                    arrayOf(
                        FO_883_2004_ART11_1,
                        FO_883_2004_ANNET,
                        FO_883_2004_ART87_8,
                        FO_883_2004_ART87A
                    ).contains(lovvalgBestemmelse)
                }
                    .toSet()

                else -> throw FunksjonellException("Støtter ikke henting av lovvalgsbestemmelser for behandlingstema ${behandlingstema}")
            }
        }
    }
}
