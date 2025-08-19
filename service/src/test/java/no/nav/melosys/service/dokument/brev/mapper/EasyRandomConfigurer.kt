package no.nav.melosys.service.dokument.brev.mapper

import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker
import no.nav.dok.brevdata.felles.v1.navfelles.NorskPostadresse
import no.nav.dok.brevdata.felles.v1.navfelles.Person
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters

object EasyRandomConfigurer {

    private val MOTTAKER: Mottaker = Person()
        .withId("foobar")
        .withTypeKode(AktoerType.PERSON)
        .withNavn("Foobar Zot")
        .withKortNavn("fbz")
        .withSpraakkode(Spraakkode.NN)
        .withMottakeradresse(
            NorskPostadresse()
                .withAdresselinje1("Gate 1")
                .withPostnummer("1234")
                .withPoststed("Sted")
                .withLand("NO")
        )

    fun paramForDokProd(): EasyRandomParameters =
        EasyRandomParameters().randomize(Mottaker::class.java) { MOTTAKER }

    fun randomForDokProd(): EasyRandom = EasyRandom(paramForDokProd())
}
