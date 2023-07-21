package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FerdigbehandlingKontrollTest {

    companion object {
        val NOW = LocalDate.now()
    }

    @Test
    internal fun utførKontroll_USA_ART5_4PeriodenErMerEnn12Måneder_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4
            fom = NOW
            tom = NOW.plusMonths(12)
        }
        val kontrollData = FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOver12Måneder(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MER_ENN_12_MD)
    }

    @Test
    internal fun utførKontroll_USA_ART5_2PeriodenErMerEnn5År_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2
            fom = NOW
            tom = NOW.plusYears(5)
        }
        val kontrollData = FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MER_ENN_FEM_ÅR)
    }

    @Test
    internal fun utførKontroll_USA_ART5_6PeriodenErMerEnn5År_ingenKontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6
            fom = NOW
            tom = NOW.plusYears(5)
        }
        val kontrollData = FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `overlappende periode skal gi kontrollfeil uavhengig om det er medlem eller unntaksperiode`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode().apply {
                        id = 1
                        land = "SWE"
                        status = "GYLD"
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
    }

    @Test
    fun `overlappende medlemskapsperiode skal gi kontrollfeil`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode().apply {
                        id = 1
                        land = "NOR"
                        status = "GYLD"
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.overlappendeMedlemsperiode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDLEMSKAPSPERIODER)
    }

    @Test
    fun `overlappende unntaksperiodeperiode skal gi kontrollfeil`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode().apply {
                        id = 1
                        land = "SWE"
                        status = "GYLD"
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.overlappendeUnntaksperiode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER)
    }
}
