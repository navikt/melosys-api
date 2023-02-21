package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_usa
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
            bestemmelse = Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_4
            fom = NOW
            tom = NOW.plusMonths(12)
        }
        val kontrollData = FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOver12Måneder(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MER_ENN_12_MD)
    }

    @Test
    internal fun utførKontroll_USA_ART5_2PeriodenErMerEnn5År_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_2
            fom = NOW
            tom = NOW.plusYears(5)
        }
        val kontrollData = FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MER_ENN_FEM_ÅR)
    }

    @Test
    internal fun utførKontroll_USA_ART5_6PeriodenErMerEnn5År_ingenKontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_6
            fom = NOW
            tom = NOW.plusYears(5)
        }
        val kontrollData = FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.shouldBeNull()
    }
}
