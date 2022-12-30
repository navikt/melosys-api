package no.nav.melosys.integrasjon.utbetaldata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.dokument.utbetaling.Periode
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingRequest
import no.nav.melosys.integrasjon.utbetaling.*
import no.nav.melosys.integrasjon.utbetaling.UtbetalingServiceV2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtbetalingServiceV2Test {

    private var mockRestConsumer = mockk<UtbetalingConsumerV2>()
    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private val utbetaldataServiceV2: UtbetalingServiceV2 = UtbetalingServiceV2(mockRestConsumer, objectMapper)

    @Test
    fun hentUtbetalingerBarnetrygd_medTreff_verifiserSaksopplysning() {
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
            .kilde.equals(SaksopplysningKildesystem.UTBETALDATA)

        saksopplysning
            .kilder
            .first()
            .mottattDokument.shouldNotBeNull().shouldNotBeEmpty()

        saksopplysning.dokument
            .shouldBeInstanceOf<UtbetalingDokument>()
            .utbetalinger.shouldHaveSize(2)
            .first()
            .ytelser.shouldHaveSize(2)
            .first()
            .shouldBeEqualToComparingFields(
                no.nav.melosys.domain.dokument.utbetaling.Ytelse().apply {
                    type = "BARNETRYGD"
                    periode = Periode(LocalDate.parse("2022-12-19"), LocalDate.parse("2022-12-19"))
                }
                , FieldsEqualityCheckConfig(ignorePrivateFields = false)
            )
    }

    @Test
    fun hentUtbetalingerBarnetrygd_utenGyldigeYtelserIUtbetaling_verifiserSaksopplysning() {
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

        saksopplysning.dokument
            .shouldBeInstanceOf<UtbetalingDokument>()
            .utbetalinger.shouldHaveSize(2)
            .last()
            .ytelser.shouldHaveSize(0)
    }

    @Test
    fun hentUtbetalingerBarnetrygd_tomDatoEldreEnnTreÅr_forventTomResponsIngenKall() {
        val fom = LocalDate.now().minusYears(5).minusMonths(2)
        val tom = LocalDate.now().minusYears(3).minusDays(1)

        val utbetalingRequest = UtbetalingRequest(FNR,
            no.nav.melosys.integrasjon.utbetaldata.utbetaling.Periode(fom.toString(), tom.toString()),
            "UTBETALINGSPERIODE",
            "RETTIGHETSHAVER")

        every {
            mockRestConsumer.hentUtbetalingsInformasjon(utbetalingRequest)
        } returns hentUtbetalingListe()

        val saksopplysning = utbetaldataServiceV2.hentSaksopplysningForUtbetaling(FNR,
            fom,
            tom)

        saksopplysning.dokument
            .shouldBeInstanceOf<UtbetalingDokument>()
            .utbetalinger.shouldHaveSize(0)
    }

    @Test
    fun hentUtbetalingerBarnetrygd_fomDatoEldreEnnTreÅrTomDatoIDag_forventKallMedFomTreÅrSiden() {
        val fom = LocalDate.now().minusYears(5).minusMonths(2)
        val tom = LocalDate.now().minusDays(1)

        val utbetalingRequest = UtbetalingRequest(FNR,
            no.nav.melosys.integrasjon.utbetaldata.utbetaling.Periode(fom.toString(), tom.toString()),
            "UTBETALINGSPERIODE",
            "RETTIGHETSHAVER")

        every {
            mockRestConsumer.hentUtbetalingsInformasjon(utbetalingRequest)
        } returns hentUtbetalingListe()

        val saksopplysning = utbetaldataServiceV2.hentSaksopplysningForUtbetaling(FNR,
            fom,
            tom)

        saksopplysning.dokument
            .shouldBeInstanceOf<UtbetalingDokument>()
            .utbetalinger.shouldHaveSize(2)
    }

    private fun hentUtbetalingListe(): List<Utbetaling> = objectMapper.readValue(
        javaClass.classLoader.getResource("mock/utbetaldata/ubetalingResponse.json"),
        Array<Utbetaling>::class.java
    ).toList()

    companion object {
        private const val FNR = "77777777773"
    }
}
