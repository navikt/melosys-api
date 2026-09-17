package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto

internal object BehandlingstemaUtleder {

    fun utled(dto: UtsendtArbeidstakerSkjemaM2MDto): Behandlingstema {
        return if (dto.erOffentligArbeidsgiver()) Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        else Behandlingstema.UTSENDT_ARBEIDSTAKER
    }
}
