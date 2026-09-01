package no.nav.melosys.service.soknad

import mu.KotlinLogging
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

private val log = KotlinLogging.logger { }

/**
 * Avgjør om arbeidsgiver og arbeidstaker har oppgitt ulik utsendingsperiode i innsendt skjemadata.
 */
@Service
class UlikPeriodeUtleder(
    private val skjemaSakMappingRepository: SkjemaSakMappingRepository
) {
    private val jsonMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @Transactional(readOnly = true)
    fun harUlikPeriode(mottatteOpplysninger: MottatteOpplysninger?): Boolean {
        val mottatteOpplysningerId = mottatteOpplysninger?.id ?: return false

        return skjemaSakMappingRepository.findByMottatteOpplysninger_Id(mottatteOpplysningerId)
            .maxByOrNull { it.opprettetDato }
            ?.let { mapping ->
                runCatching { jsonMapper.readValue<UtsendtArbeidstakerSkjemaM2MDto>(mapping.originalData) }
                    .onFailure {
                        log.warn(it) {
                            "Kunne ikke lese skjemadata for skjemaId=${mapping.skjemaId} — varsler ikke om ulik periode"
                        }
                    }
                    .getOrNull()
            }
            ?.let(::harUlikPeriode)
            ?: false
    }

    /**
     * Sender partene inn hver for seg, ligger motpartens skjema på `kobletSkjema`. Sender de inn
     * samlet, deler de ett periodefelt og kan ikke ha avvik.
     */
    private fun harUlikPeriode(dto: UtsendtArbeidstakerSkjemaM2MDto): Boolean {
        val egenPeriode = dto.skjema.data.utsendingsperiodeOgLand?.utsendelsePeriode ?: return false
        val motpartensPeriode = dto.kobletSkjema?.data?.utsendingsperiodeOgLand?.utsendelsePeriode ?: return false
        return egenPeriode != motpartensPeriode
    }
}
