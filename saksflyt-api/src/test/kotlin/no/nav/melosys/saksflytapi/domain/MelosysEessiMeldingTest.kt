package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.Test

class MelosysEessiMeldingTest {
    @Test
    fun `serialisering og deserialisering av EESSI melding skal bevare data`() {
        val easyRandomParameters = EasyRandomParameters().collectionSizeRange(1, 2).stringLengthRange(1, 4)
        val eessiMelding = EasyRandom(easyRandomParameters).nextObject(MelosysEessiMelding::class.java)
        val p = Prosessinstans.forTest { }

        p.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)
        val deserialisering = p.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java)

        deserialisering shouldBe eessiMelding
    }
}
