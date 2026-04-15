package no.nav.melosys.skjema.types.utsendtarbeidstaker

import java.time.LocalDateTime
import java.util.UUID
import no.nav.melosys.skjema.types.SkjemaDto
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class UtsendtArbeidstakerSkjemaDto(
    override val id: UUID,
    override val status: SkjemaStatus,
    override val type: SkjemaType = SkjemaType.UTSENDT_ARBEIDSTAKER,
    override val fnr: String,
    override val orgnr: String,
    override val opprettetDato: LocalDateTime,
    override val endretDato: LocalDateTime,
    override val metadata: UtsendtArbeidstakerMetadata,
    override val data: UtsendtArbeidstakerSkjemaData
) : SkjemaDto
