package no.nav.melosys.service.dokument.sed.mapper

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.SAERLIG_GRUNN
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_engelsk_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_engelsk_begrunnelser
import no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk
import org.junit.jupiter.api.Test

class VilkaarsresultatTilBegrunnelseMapperKtTest {

    @Test
    fun tilEngelskBegrunnelseString_medArt16_1_forventBeskrivelse() {
        val vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(listOf(UTSENDELSE_MELLOM_24_MN_OG_5_AAR.kode))

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe
            Anmodning_engelsk_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.beskrivelse
    }

    @Test
    fun tilEngelskBegrunnelseString_medArt16UtenArt12_forventBeskrivelse() {
        val vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(listOf(SJOEMANNSKIRKEN.kode))

        val result = VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat)
        result shouldBe "Working for Sjømannskirken (The Norwegian Seamen’s Church), which is a nonprofit organization receiving financial support from the Norwegian Government."
    }

    @Test
    fun testArt161Anmodning_motArt161AnmodningEngelsk() {
        val begrunnelserArt16 = hentAlleVerdierFraKodeverk(Anmodning_begrunnelser::class.java)
        val begrunnelserArt16Engelsk = hentAlleVerdierFraKodeverk(Anmodning_engelsk_begrunnelser::class.java)

        begrunnelserArt16.toList() shouldContainExactly begrunnelserArt16Engelsk.toList()
    }

    @Test
    fun testArt16AnmodningUtenArt12_motArt16AnmodningUtenArt12Engelsk() {
        val begrunnelserArt16Uten12 = hentAlleVerdierFraKodeverk(Direkte_til_anmodning_begrunnelser::class.java)
        val begrunnelserArt16Uten12Engelsk = hentAlleVerdierFraKodeverk(Direkte_til_anmodning_engelsk_begrunnelser::class.java)

        begrunnelserArt16Uten12.toList() shouldContainExactly begrunnelserArt16Uten12Engelsk.toList()
    }

    @Test
    fun testArt161anmodning_motArt161AnmodningUtenArt12Engelsk() {
        val begrunnelserArt16Engelsk =
            hentAlleVerdierFraKodeverk(Anmodning_engelsk_begrunnelser::class.java).toList().toSet()
        val begrunnelserArt16Uten12Engelsk =
            hentAlleVerdierFraKodeverk(Direkte_til_anmodning_engelsk_begrunnelser::class.java).toList().toSet()

        // Ok å ha samme kode i begge listene, så lenge den engelske beskrivelsen også er lik
        val koderTilstedeIBeggeLister = begrunnelserArt16Engelsk.toMutableSet()
        koderTilstedeIBeggeLister.retainAll(begrunnelserArt16Uten12Engelsk)
        koderTilstedeIBeggeLister.remove(SAERLIG_GRUNN.kode)

        for (kode in koderTilstedeIBeggeLister) {
            val art16Beskrivelse_engelsk = Anmodning_engelsk_begrunnelser.valueOf(kode).beskrivelse
            val art16UtenArt12Beskrivelse_engelsk = Direkte_til_anmodning_engelsk_begrunnelser.valueOf(kode).beskrivelse

            art16Beskrivelse_engelsk shouldBe art16UtenArt12Beskrivelse_engelsk
        }
    }

    @Test
    fun tilEngelskBegrunnelseString_Art16MedFlereBegrunnelser_forventSammensattBeskrivelse() {
        val vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(
            listOf(
                "UTSENDELSE_MELLOM_24_MN_OG_5_AAR",
                "IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK"
            )
        )

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe
            Anmodning_engelsk_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.beskrivelse + "\n" +
            Anmodning_engelsk_begrunnelser.IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK.beskrivelse
    }

    @Test
    fun tilEngelskBegrunnelseString_art16_forventFritekst() {
        val vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(listOf("SAERLIG_GRUNN"))
        vilkaarsresultat.begrunnelseFritekstEessi = "Fritekst som beskriver anmodning om unntak"

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe
            "Fritekst som beskriver anmodning om unntak"
    }

    @Test
    fun tilEngelskBegrunnelseString_Art16MedFlereBegrunnelserOgFritekst_forventSammensattBeskrivelseOgFritekst() {
        val vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(
            listOf(
                "UTSENDELSE_MELLOM_24_MN_OG_5_AAR",
                "IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK",
                "SAERLIG_GRUNN"
            )
        )
        val fritekstEngelsk = "Something"
        vilkaarsresultat.begrunnelseFritekstEessi = fritekstEngelsk

        val result = VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat)
        result shouldBe Anmodning_engelsk_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.beskrivelse + "\n" +
            Anmodning_engelsk_begrunnelser.IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK.beskrivelse + "\n" +
            fritekstEngelsk
    }

    @Test
    fun tilEngelskBegrunnelseString_MedKodeSomIkkeFinnes_forventTomString() {
        val vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(listOf("EN_KODE_SOM_IKKE_FINNES_I_KODEVERK"))

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe ""
    }

    private fun lagVilkaarsresultatMedBegrunnelser(vilkaarBegrunnelseKoder: List<String>): Vilkaarsresultat {
        val vilkaarBegrunnelser = vilkaarBegrunnelseKoder.map { lagVilkaarBegrunnelse(it) }.toSet()

        return Vilkaarsresultat().apply {
            begrunnelser = vilkaarBegrunnelser
        }
    }

    private fun lagVilkaarBegrunnelse(kode: String): VilkaarBegrunnelse {
        return VilkaarBegrunnelse().apply {
            this.kode = kode
        }
    }
}
