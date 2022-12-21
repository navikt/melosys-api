package no.nav.melosys.integrasjon.utbetaldata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.dokument.utbetaling.Periode
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingRequest
import no.nav.melosys.integrasjon.utbetaling.*
import no.nav.melosys.integrasjon.utbetaling.UtbetalingServiceV2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtbetalingServiceV2 {

    private var mockRestConsumer = mockk<UtbetalingConsumerV2>()
    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private val utbetaldataServiceV2: UtbetalingServiceV2 = UtbetalingServiceV2(mockRestConsumer, objectMapper)

    @Test
    fun skalHenteUtbetalinger() {
        val fom = LocalDate.now().minusMonths(2).toString()
        val tom = LocalDate.now().toString()

        val utbetalingRequest = UtbetalingRequest(FNR,
            no.nav.melosys.integrasjon.utbetaldata.utbetaling.Periode(fom, tom),
            "UTBETALINGSPERIODE",
            "RETTIGHETSHAVER")

        every {
            mockRestConsumer.hentUtbetalingsInformasjon(utbetalingRequest)
        } returns hentUtbetalingListe()

        val saksopplysning = utbetaldataServiceV2.hentSaksopplysningForUtbetaling(FNR,
            LocalDate.now().minusMonths(2),
            LocalDate.now())

        saksopplysning
            .shouldNotBeNull()
            .kilder
            .shouldHaveSize(1)
            .first()
            .mottattDokument.isNullOrEmpty()

        saksopplysning.dokument
            .shouldBeInstanceOf<UtbetalingDokument>()
            .utbetalinger.shouldHaveSize(2)
            .first()
            .ytelser.shouldHaveSize(1)
            .first()
            .shouldBeEqualToComparingFields(
                no.nav.melosys.domain.dokument.utbetaling.Ytelse().apply {
                    type = "string"
                    periode = Periode(LocalDate.parse("2022-12-19"), LocalDate.parse("2022-12-19"))
                }
                , FieldsEqualityCheckConfig(ignorePrivateFields = false)
            )
    }

    private fun hentUtbetalingListe(): List<Utbetaling> = objectMapper.readValue(
        javaClass.classLoader.getResource("mock/utbetaldata/ubetalingResponse.json"),
        Array<Utbetaling>::class.java
    ).toList()

    @Test
    @Throws(Exception::class)
    fun hentUtbetalingerBarnetrygd_medTreff_verifiserSaksopplysning() {

    }

    @Test
    @Throws(Exception::class)
    fun hentUtbetalingerBarnetrygd_medForskjelligeYtelserIEnUtbetaling_verifiserSaksopplysning() {

    }

    @Test
    fun hentUtbetalingerBarnetrygd_tomDatoEldreEnnTreÅr_forventTomResponsIngenKall() {
        val fom = LocalDate.now().minusMonths(2).toString()
        val tom = LocalDate.now().toString()

        val utbetalingRequest = UtbetalingRequest(FNR,
            no.nav.melosys.integrasjon.utbetaldata.utbetaling.Periode(fom, tom),
            "UTBETALINGSPERIODE",
            "RETTIGHETSHAVER")

        every {
            mockRestConsumer.hentUtbetalingsInformasjon(utbetalingRequest)
        } returns hentUtbetalingListe()

        val saksopplysning = utbetaldataServiceV2.hentSaksopplysningForUtbetaling(FNR,
            LocalDate.now().minusYears(5).minusMonths(2),
            LocalDate.now().minusYears(3).minusDays(1))

        saksopplysning.dokument
            .shouldBeInstanceOf<UtbetalingDokument>()
            .utbetalinger.shouldHaveSize(0)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun hentUtbetalingerBarnetrygd_fomDatoEldreEnnTreÅrTomDatoIDag_forventKallMedFomTreÅrSiden() {

    }

    companion object {
        private const val FNR = "77777777773"
    }
}
