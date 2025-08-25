package no.nav.melosys.domain.avklartefakta

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class AvklartefaktaTest {

    private fun lagRegistrering(begrunnelse: String, avklartefakta: Avklartefakta) =
        AvklartefaktaRegistrering().apply {
            begrunnelseKode = begrunnelse
            this.avklartefakta = avklartefakta
        }

    @Test
    fun testOppdaterMedEkstraRegistrering() {
        val opphold1 = "Opphold"
        val opphold2 = "Opphold"
        val familie = "Familie"
        val avklartefakta = Avklartefakta()
        val førsteRegistrering = lagRegistrering(opphold1, avklartefakta)
        avklartefakta.registreringer = hashSetOf(førsteRegistrering)

        val nyeRegistreringer = hashSetOf(
            lagRegistrering(opphold2, avklartefakta),
            lagRegistrering(familie, avklartefakta)
        )


        avklartefakta.oppdaterRegistreringer(nyeRegistreringer)


        avklartefakta.registreringer.run {
            shouldHaveSize(2)
            shouldContain(førsteRegistrering)
        }
    }
}