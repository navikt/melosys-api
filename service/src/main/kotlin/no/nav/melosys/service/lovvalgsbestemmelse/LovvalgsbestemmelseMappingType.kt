package no.nav.melosys.service.lovvalgsbestemmelse

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException

enum class LovvalgsbestemmelseMappingType {
    YRKESAKTIV, IKKE_YRKESAKTIV, UNNTAK;

    companion object {
        fun utledType(sakstema: Sakstemaer, behandlingstema: Behandlingstema): LovvalgsbestemmelseMappingType {
            if (sakstema === Sakstemaer.MEDLEMSKAP_LOVVALG && Behandlingstema.YRKESAKTIV === behandlingstema)
                return YRKESAKTIV

            if (sakstema === Sakstemaer.MEDLEMSKAP_LOVVALG && Behandlingstema.IKKE_YRKESAKTIV === behandlingstema)
                return IKKE_YRKESAKTIV

            if (sakstema === Sakstemaer.UNNTAK && (Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL === behandlingstema || Behandlingstema.REGISTRERING_UNNTAK === behandlingstema))
                return UNNTAK

            throw FunksjonellException("Kan ikke mappe lovvalgsbestemmelser for sakstema=${sakstema} og behandlingstema=${behandlingstema}")
        }
    }
}
