package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.utbetaling.Periode
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.dokument.utbetaling.Ytelse
import no.nav.melosys.exception.TekniskException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.function.Consumer
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

@Service
class UtbetaldataRestService(
    private val utbetaldataRestConsumer: UtbetaldataRestConsumer,
    private val objectMapper: ObjectMapper
) {

    init {
        objectMapper.registerKotlinModule()
    }

    fun hentUtbetalingerBarnetrygd(fnr: String, fom: LocalDate, tom: LocalDate): Saksopplysning {
        val utbetalingRequest = UtbetalingRequest(fnr,
            Periode(fom.toString(), tom.toString()),
            "UTBETALINGSPERIODE",
            "RETTIGHETSHAVER")

        val utbetalingResponse = if (erTomEldreEnnTreAar(fnr, fom, tom))
            emptyList()
        else
            fjernYtelserFraUtbetalingerSomIkkeErBarnetrygd(utbetaldataRestConsumer.hentUtbetalingsInformasjon(utbetalingRequest))

        return Saksopplysning().apply {
            type = SaksopplysningType.UTBETAL
            versjon = BETALINGER_VERSJON
            dokument = UtbetalingDokument().apply {
                utbetalinger = utbetalingResponse.map {
                    Utbetaling().apply {
                        ytelser = it.ytelseListe.map {
                            Ytelse().apply {
                                type = it.ytelsestype
                                periode = Periode(LocalDate.parse(it.ytelsesperiode.fom), LocalDate.parse(it.ytelsesperiode.tom))
                            }
                        }
                    }
                }
            }
            try {
                leggTilKildesystemOgMottattDokument(
                    SaksopplysningKildesystem.UTBETALDATA,
                    objectMapper.writeValueAsString(utbetalingResponse)
                )
            } catch (e: JsonProcessingException) {
                throw TekniskException("Kunne ikke lagre kildedokument fra utbetaldata")
            }
        }
    }

    fun erTomEldreEnnTreAar(fnr: String, fom: LocalDate, tom: LocalDate): Boolean {
        return (tom.isBefore(LocalDate.now().minusYears(3)))
    }

    private fun fjernYtelserFraUtbetalingerSomIkkeErBarnetrygd(response: List<no.nav.melosys.integrasjon.utbetaling.Utbetaling>): List<no.nav.melosys.integrasjon.utbetaling.Utbetaling> {
        log.info { "Henter utbetalinger for barnetrygd: " + response.size }
        response.forEach(Consumer { utbetaling: no.nav.melosys.integrasjon.utbetaling.Utbetaling ->
            utbetaling.ytelseListe
                .removeIf { ytelse: no.nav.melosys.integrasjon.utbetaling.Ytelse -> ytelse.ytelsestype != BARNETRYGD }
        })
        return response
    }

    companion object {
        const val BETALINGER_VERSJON = "2.0"
        const val BARNETRYGD = "BARNETRYGD"
    }

}



