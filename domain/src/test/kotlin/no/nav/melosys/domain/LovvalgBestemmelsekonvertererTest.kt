package no.nav.melosys.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import org.junit.jupiter.api.Test

internal class LovvalgBestemmelsekonvertererTest {

    private val instans = LovvalgBestemmelsekonverterer()

    @Test
    fun konverterFra883_2004TilDbKolonneGirStreng() {
        testConvertToDatabaseColumn(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, "ART16_2")
    }

    @Test
    fun konverterFra987_2009TilDbKolonneGirStreng() {
        testConvertToDatabaseColumn(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11, "ART14_11")
    }

    @Test
    fun konverterFraTillegg883_2004TilDbKolonneGirStreng() {
        testConvertToDatabaseColumn(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2, "ART11_2")
    }

    @Test
    fun konverterFraNullTilDbKolonneGirNull() {
        testConvertToDatabaseColumn(null, null)
    }

    private fun testConvertToDatabaseColumn(input: LovvalgBestemmelse?, forventet: String?) {
        val resultat = instans.convertToDatabaseColumn(input)
        if (input == null) {
            resultat.shouldBeNull()
        } else {
            resultat shouldEndWith forventet!!
        }
    }

    @Test
    fun konverter883_2004TilEntitsattributtGirOppramsInstans() {
        testKonverterTilEntitetsAttributt(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2)
    }

    @Test
    fun konverter987_2009TilEntitsattributtGirOppramsInstans() {
        testKonverterTilEntitetsAttributt(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11)
    }

    @Test
    fun konverterTillegg883_2004TilEntitsattributtGirOppramsInstans() {
        testKonverterTilEntitetsAttributt(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A)
    }

    @Test
    fun konverterNullTilEntitsattributtGirNull() {
        testKonverterTilEntitetsAttributt(null)
    }

    private fun testKonverterTilEntitetsAttributt(input: LovvalgBestemmelse?) {
        val resultat = instans.convertToEntityAttribute(input?.name())
        resultat shouldBe input
    }

    @Test
    fun konverterUkjentOppramsverdiKasterUnntak() {
        val thrown = shouldThrow<IllegalArgumentException> {
            instans.convertToEntityAttribute("Brottskavl")
        }
        thrown.run {
            shouldBeInstanceOf<IllegalArgumentException>()
            message shouldContain "Lovvalgbestemmelse kode:Brottskavl ikke funnet"
        }
    }
}