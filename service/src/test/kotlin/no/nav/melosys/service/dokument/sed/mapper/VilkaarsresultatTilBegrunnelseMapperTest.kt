package no.nav.melosys.service.dokument.sed.mapper

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.begrunnelse
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.SAERLIG_GRUNN
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_engelsk_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_engelsk_begrunnelser
import no.nav.melosys.domain.vilkaarsresultatForTest
import no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.Companion.hentAlleVerdierFraKodeverk
import org.junit.jupiter.api.Test

class VilkaarsresultatTilBegrunnelseMapperTest {

    @Test
    fun tilEngelskBegrunnelseString_medArt16_1_forventBeskrivelse() {
        val vilkaarsresultat = vilkaarsresultatForTest {
            begrunnelse(UTSENDELSE_MELLOM_24_MN_OG_5_AAR.kode)
        }

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe
            Anmodning_engelsk_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.beskrivelse
    }

    @Test
    fun tilEngelskBegrunnelseString_medArt16UtenArt12_forventBeskrivelse() {
        val vilkaarsresultat = vilkaarsresultatForTest {
            begrunnelse(SJOEMANNSKIRKEN.kode)
        }

        val result = VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat)
        result shouldBe Direkte_til_anmodning_engelsk_begrunnelser.SJOEMANNSKIRKEN.beskrivelse
    }

    @Test
    fun testArt161Anmodning_motArt161AnmodningEngelsk() {
        val begrunnelserArt16 = hentAlleVerdierFraKodeverk(Anmodning_begrunnelser::class)
        val begrunnelserArt16Engelsk = hentAlleVerdierFraKodeverk(Anmodning_engelsk_begrunnelser::class)

        begrunnelserArt16.toList() shouldContainExactly begrunnelserArt16Engelsk.toList()
    }

    @Test
    fun testArt16AnmodningUtenArt12_motArt16AnmodningUtenArt12Engelsk() {
        val begrunnelserArt16Uten12 = hentAlleVerdierFraKodeverk(Direkte_til_anmodning_begrunnelser::class)
        val begrunnelserArt16Uten12Engelsk = hentAlleVerdierFraKodeverk(Direkte_til_anmodning_engelsk_begrunnelser::class)

        begrunnelserArt16Uten12.toList() shouldContainExactly begrunnelserArt16Uten12Engelsk.toList()
    }

    @Test
    fun testArt161anmodning_motArt161AnmodningUtenArt12Engelsk() {
        val begrunnelserArt16Engelsk =
            hentAlleVerdierFraKodeverk(Anmodning_engelsk_begrunnelser::class).toList().toSet()
        val begrunnelserArt16Uten12Engelsk =
            hentAlleVerdierFraKodeverk(Direkte_til_anmodning_engelsk_begrunnelser::class).toList().toSet()

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
        val vilkaarsresultat = vilkaarsresultatForTest {
            begrunnelse("UTSENDELSE_MELLOM_24_MN_OG_5_AAR")
            begrunnelse("IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK")
        }

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe
            Anmodning_engelsk_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.beskrivelse + "\n" +
            Anmodning_engelsk_begrunnelser.IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK.beskrivelse
    }

    @Test
    fun tilEngelskBegrunnelseString_art16_forventFritekst() {
        val vilkaarsresultat = vilkaarsresultatForTest {
            begrunnelse("SAERLIG_GRUNN")
            begrunnelseFritekstEessi = "Fritekst som beskriver anmodning om unntak"
        }

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe
            "Fritekst som beskriver anmodning om unntak"
    }

    @Test
    fun tilEngelskBegrunnelseString_Art16MedFlereBegrunnelserOgFritekst_forventSammensattBeskrivelseOgFritekst() {
        val fritekstEngelsk = "Something"
        val vilkaarsresultat = vilkaarsresultatForTest {
            begrunnelse("UTSENDELSE_MELLOM_24_MN_OG_5_AAR")
            begrunnelse("IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK")
            begrunnelse("SAERLIG_GRUNN")
            begrunnelseFritekstEessi = fritekstEngelsk
        }

        val result = VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat)
        result shouldBe Anmodning_engelsk_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.beskrivelse + "\n" +
            Anmodning_engelsk_begrunnelser.IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK.beskrivelse + "\n" +
            fritekstEngelsk
    }

    @Test
    fun tilEngelskBegrunnelseString_MedKodeSomIkkeFinnes_forventTomString() {
        val vilkaarsresultat = vilkaarsresultatForTest {
            begrunnelse("EN_KODE_SOM_IKKE_FINNES_I_KODEVERK")
        }

        VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat) shouldBe ""
    }
}
