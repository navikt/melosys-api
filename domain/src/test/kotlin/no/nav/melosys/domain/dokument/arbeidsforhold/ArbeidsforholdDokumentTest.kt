package no.nav.melosys.domain.dokument.arbeidsforhold

import io.kotest.matchers.collections.*
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
        }.also { arbeidsforholdDokument.arbeidsforhold.add(it) }

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
}
