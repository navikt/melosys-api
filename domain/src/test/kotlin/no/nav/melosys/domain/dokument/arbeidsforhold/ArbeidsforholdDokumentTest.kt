package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.melosys.domain.dokument.felles.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsforholdDokumentTest {
    private val arbeidsforholdDokument = ArbeidsforholdDokument()
    private val eksisterendePeriode = Periode(LocalDate.now(), LocalDate.now())
    private val orgNr1 = "12345678910"
    private val orgNr2 = "10987654321"

    init {
        leggTilArbeidsforhold(orgNr1, null, eksisterendePeriode)
    }

    private fun leggTilArbeidsforhold(arbeidsgiverID: String?, opplysningspliktigID: String?, periode: Periode?): Arbeidsforhold =
        Arbeidsforhold().apply {
            this.arbeidsgiverID = arbeidsgiverID
            this.opplysningspliktigID = opplysningspliktigID
            ansettelsesPeriode = periode
        }.also { arbeidsforholdDokument.arbeidsforhold += it }

    @Test
    fun hentAnsettelsesperioderIngenValgteOrgnummer() {
        val tomListeMedOrgnumre = emptyList<String>()


        val perioder = arbeidsforholdDokument.hentAnsettelsesperioder(tomListeMedOrgnumre)


        perioder.shouldBeEmpty()
    }

    @Test
    fun hentAnsettelsesperioderKunUtvalgteOrgnumre() {
        leggTilArbeidsforhold(orgNr2, null, Periode(LocalDate.now(), LocalDate.now()))
        val orgnumre = listOf(orgNr1)


        val perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre)


        perioder shouldContainOnly setOf(eksisterendePeriode)
    }

    @Test
    fun hentAnsettelsesperioderOpplysningspliktigOrgnumre() {
        val forventetPeriode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
        leggTilArbeidsforhold(null, orgNr2, forventetPeriode)
        val orgnumre = listOf(orgNr2)


        val perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre)


        perioder shouldContainOnly setOf(forventetPeriode)
    }

    @Test
    fun hentAnsettelsesperioder() {
        val nyPeriode = Periode(LocalDate.now(), LocalDate.now())
        leggTilArbeidsforhold(orgNr2, null, nyPeriode)
        val orgnumre = listOf(orgNr1, orgNr2)


        val perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre)


        perioder shouldContain eksisterendePeriode
        perioder shouldContain nyPeriode
    }

    @Test
    fun hentAnsettelsesperioderFiltrererUdefinertePerioder() {
        leggTilArbeidsforhold(orgNr2, null, null)
        val orgnumre = listOf(orgNr1, orgNr2)


        val perioder = arbeidsforholdDokument.hentAnsettelsesperioder(orgnumre)


        perioder.run {
            shouldHaveSize(1)
            shouldContainOnly(setOf(eksisterendePeriode))
        }
    }

    @Test
    fun hentAlleOrgnumre() {
        val orgNr3 = "123123123"
        leggTilArbeidsforhold(orgNr2, orgNr3, Periode(LocalDate.now(), LocalDate.now()))


        val orgnumre = arbeidsforholdDokument.hentOrgnumre()


        orgnumre shouldContainExactlyInAnyOrder listOf(orgNr1, orgNr2, orgNr3)
    }

    @Test
    fun hentArbeidsgiverIDer() {
        val orgNr3 = "987654321"
        leggTilArbeidsforhold(orgNr2, null, Periode(LocalDate.now(), LocalDate.now()))
        leggTilArbeidsforhold(orgNr3, null, Periode(LocalDate.now(), LocalDate.now()))


        val arbeidsgiverIDer = arbeidsforholdDokument.hentArbeidsgiverIDer()


        arbeidsgiverIDer shouldContainExactlyInAnyOrder listOf(orgNr1, orgNr2, orgNr3)
    }

    @Test
    fun hentArbeidsgiverIDerFiltersBlankeVerdier() {
        leggTilArbeidsforhold("", null, Periode(LocalDate.now(), LocalDate.now()))
        leggTilArbeidsforhold(null, null, Periode(LocalDate.now(), LocalDate.now()))


        val arbeidsgiverIDer = arbeidsforholdDokument.hentArbeidsgiverIDer()


        arbeidsgiverIDer shouldContainOnly setOf(orgNr1)
    }

    @Test
    fun jsonSerialiseringOgDeserialiseringBrukerArbeidsforholdListe() {
        val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        val arbeidsforhold1 = Arbeidsforhold().apply {
            arbeidsgiverID = "123456789"
            ansettelsesPeriode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
        }
        val arbeidsforhold2 = Arbeidsforhold().apply {
            arbeidsgiverID = "987654321"
            ansettelsesPeriode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        }
        val dokument = ArbeidsforholdDokument(listOf(arbeidsforhold1, arbeidsforhold2))


        val json = objectMapper.writeValueAsString(dokument)
        val deserialisert: ArbeidsforholdDokument = objectMapper.readValue(json)


        deserialisert.arbeidsforhold.size shouldBe 2
        deserialisert.arbeidsforhold[0].arbeidsgiverID shouldBe "123456789"
        deserialisert.arbeidsforhold[1].arbeidsgiverID shouldBe "987654321"
    }

    @Test
    fun jsonSerialiseringMedJsonValueAnnotationGirListeIkkeObjekt() {
        val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        val arbeidsforhold1 = Arbeidsforhold().apply {
            arbeidsgiverID = "123456789"
            ansettelsesPeriode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
        }
        val dokument = ArbeidsforholdDokument(listOf(arbeidsforhold1))


        val json = objectMapper.writeValueAsString(dokument)


        // Med @JsonValue skal JSON være en liste, ikke et objekt med "arbeidsforhold"-felt
        json.trim().shouldStartWith("[")
        json.trim().shouldEndWith("]")
        json.shouldNotContain("\"arbeidsforhold\"")
    }

    @Test
    fun jsonDeserialiseringMedJsonCreatorAnnotationFraListeFormat() {
        val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        // JSON i liste-format (slik det var i Java med @JsonValue)
        val json = """
            [
                {
                    "arbeidsgiverID": "123456789",
                    "ansettelsesPeriode": {
                        "fom": "2023-01-01",
                        "tom": "2023-12-31"
                    }
                }
            ]
        """.trimIndent()


        val deserialisert: ArbeidsforholdDokument = objectMapper.readValue(json)


        // Med @JsonCreator skal deserialisering fra liste-format fungere
        deserialisert.arbeidsforhold.size shouldBe 1
        deserialisert.arbeidsforhold[0].arbeidsgiverID shouldBe "123456789"
    }
}
