package no.nav.melosys.tjenester.gui.dto.saksopplysninger

import io.kotest.matchers.shouldBe
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto.medlemsperiodeKomparator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SaksopplysningerTilDtoTest {

    @Test
    fun `skal teste arbeidsforhold sortering`() {
        val arbeidsforholdListe = mutableListOf<Arbeidsforhold>()
        val a1 = Arbeidsforhold().apply {
            ansettelsesPeriode = Periode(LocalDate.now(), LocalDate.MAX)
        }
        arbeidsforholdListe.add(a1)
        
        val a2 = Arbeidsforhold().apply {
            ansettelsesPeriode = Periode(LocalDate.now(), null)
        }
        arbeidsforholdListe.add(a2)
        
        val a3 = Arbeidsforhold().apply {
            ansettelsesPeriode = Periode(LocalDate.now().plusYears(1), null)
        }
        arbeidsforholdListe.add(a3)
        
        val a4 = Arbeidsforhold().apply {
            ansettelsesPeriode = Periode(LocalDate.now().plusYears(2), LocalDate.MAX)
        }
        arbeidsforholdListe.add(a4)

        val arbeidsforholdComparator = SaksopplysningerTilDto.ArbeidsforholdComparator()
        arbeidsforholdListe.sortWith(arbeidsforholdComparator)
        
        
        arbeidsforholdListe.run {
            get(0) shouldBe a3
            get(size - 1) shouldBe a1
        }
    }

    @Test
    fun `skal teste medlemsperioder kronologisk`() {
        val medlemsperioder = mutableListOf<Medlemsperiode>()
        
        val medlemsperiode1 = Medlemsperiode(
            null,
            no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2005, 1, 1), LocalDate.of(2006, 5, 30)),
            "PMMEDSKP",
            null, null, null, null, null, null, null
        )

        val medlemsperiode2 = Medlemsperiode(
            null,
            no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2016, 1, 1), LocalDate.of(2016, 12, 31)),
            "PUMEDSKP",
            null, null, null, null, null, null, null
        )

        val medlemsperiode3 = Medlemsperiode(
            null,
            no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2017, 1, 1), LocalDate.of(2017, 12, 31)),
            "PUMEDSKP",
            null, null, null, null, null, null, null
        )

        val medlemsperiode4 = Medlemsperiode(
            null,
            no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31)),
            "PMMEDSKP",
            null, null, null, null, null, null, null
        )

        medlemsperioder.apply {
            add(medlemsperiode1)
            add(medlemsperiode2)
            add(medlemsperiode3)
            add(medlemsperiode4)
        }

        medlemsperioder.sortWith(compareBy<Medlemsperiode> { it.type }.then(medlemsperiodeKomparator))

        
        medlemsperioder.run {
            get(0) shouldBe medlemsperiode4
            get(1) shouldBe medlemsperiode1
            get(2) shouldBe medlemsperiode3
            get(3) shouldBe medlemsperiode2
        }
    }
}