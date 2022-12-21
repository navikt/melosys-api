package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.utbetaling.Periode
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.dokument.utbetaling.Ytelse
import no.nav.melosys.exception.TekniskException
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UtbetalingServiceV2(
    private val utbetalingConsumerV2: UtbetalingConsumerV2,
    private val objectMapper: ObjectMapper
) {

    init {
        objectMapper.registerModule(JavaTimeModule())
    }

    fun hentSaksopplysningForUtbetaling(fnr: String, fom: LocalDate?, tom: LocalDate?): Saksopplysning {
        val utbetalingResponse = utbetalingConsumerV2.hentUtbetalingsInformasjon(fnr, fom.toString(), tom.toString())

        return Saksopplysning().apply {
            type = SaksopplysningType.UTBETAL
            versjon = BETALINGER_VERSJON
            dokument = UtbetalingDokument().apply {
                utbetalinger = utbetalingResponse.map {
                    Utbetaling().apply {
                        ytelser = it.ytelseListe.map {
                            Ytelse().apply {
                                type = it.ytelsestype
                                periode = Periode(it.ytelsesperiode!!.fom, it.ytelsesperiode!!.tom)
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
                throw TekniskException("Kunne ikke lagre kildedokument fra utbetaldta")
            }
        }
    }

    companion object {
        const val BETALINGER_VERSJON = "2.0"
    }

}



