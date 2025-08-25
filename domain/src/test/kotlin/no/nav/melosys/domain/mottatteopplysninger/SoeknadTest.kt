package no.nav.melosys.domain.mottatteopplysninger

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import org.junit.jupiter.api.Test

internal class SoeknadTest {
    @Test
    fun hentAlleOrganisasjonsnumre() {
        val selvstendigForetak = SelvstendigForetak().apply {
            orgnr = "12345678910"
        }
        val soeknad = Soeknad()
        soeknad.selvstendigArbeid.selvstendigForetak = listOf(selvstendigForetak)

        val orgNr2 = "10987654321"
        (soeknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere as MutableList).add(orgNr2)


        val organisasjonsnumre = soeknad.hentAlleOrganisasjonsnumre()


        organisasjonsnumre.run {
            size shouldBe 2
            shouldContain(selvstendigForetak.orgnr)
            shouldContain(orgNr2)
        }
    }
}