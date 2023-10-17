package no.nav.melosys.integrasjon.inntk

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.integrasjon.inntk.TestData.avvik
import no.nav.melosys.integrasjon.inntk.inntekt.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InntektServiceTest {

    private val inntektRestConsumer = mockk<InntektRestConsumer>()
    private lateinit var inntektService: InntektService

    @BeforeEach
    fun setup() {
        val aktoer = Aktoer(PERSON_ID, AktoerType.AKTOER_ID)
        every { inntektRestConsumer.hentInntektListe(any()) } returns InntektResponse(
            listOf(
                InntektResponse.ArbeidsInntektMaaned(
                    aarMaaned = YearMonth.of(2022, 1),
                    avvikListe = listOf(avvik.copy(ident = aktoer)),
                    arbeidsInntektInformasjon = InntektResponse.ArbeidsInntektInformasjon()
                )
            ),
            aktoer
        )

        inntektService = InntektService(inntektRestConsumer)
    }

    @Test
    fun hentInntektListe_periodeEtterJan2015_henterInntekt() {
        val saksopplysning: Saksopplysning = inntektService.hentInntektListe(
            personID = PERSON_ID,
            fom = YearMonth.of(2017, 6),
            tom = YearMonth.of(2017, 8)
        )
        val dokument = saksopplysning.dokument as InntektDokument
        Assertions.assertThat(dokument).isNotNull()
    }

    @Test
    fun hentInntektListe_fomFørJan2015_henterInntektMedFomJan2015() {
        val saksopplysning: Saksopplysning = inntektService.hentInntektListe(
            personID = PERSON_ID,
            fom = YearMonth.of(2014, 6),
            tom = YearMonth.of(2017, 8)
        )

        val dokument = saksopplysning.dokument as InntektDokument
        val slot = slot<InntektRequest>()
        verify { inntektRestConsumer.hentInntektListe(capture(slot)) }
        slot.captured.maanedFom.shouldBe(YearMonth.of(2015, 1))
        slot.captured.maanedTom.shouldBe(YearMonth.of(2017, 8))
        dokument.shouldNotBeNull()
    }

    @Test
    fun hentInntektListe_helePeriodeFørJan2015_returnererTomInntektListe() {
        val saksopplysning: Saksopplysning =
            inntektService.hentInntektListe(
                personID = PERSON_ID,
                fom = YearMonth.of(2012, 1),
                tom = YearMonth.of(2014, 12)
            )
        verify { inntektRestConsumer.hentInntektListe(any()) wasNot Called }

        saksopplysning.kilder.shouldNotBeEmpty()
        saksopplysning.kilder.iterator().next().mottattDokument.shouldNotBeNull()
        saksopplysning.dokument
            .shouldBeInstanceOf<InntektDokument>()
            .arbeidsInntektMaanedListe
            .shouldBeEmpty()
    }

    companion object {
        const val PERSON_ID = "99999999992"
    }
}
