package no.nav.melosys.service.dokument.brev.mapper.felles

import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.kodeverk.begrunnelser.*
import no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.Companion.hentAlleVerdierFraKodeverk
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class VilkaarbegrunnelseFactoryTest {

    @Test
    fun `mapArt121BegrunnelseType skal mappe alle utsendt arbeidstaker begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Utsendt_arbeidstaker_begrunnelser::class)


        VilkaarbegrunnelseFactory.mapArt121BegrunnelseType(begrunnelser)
    }

    @Test
    fun `mapArt121ForugåendeBegrunnelse skal mappe alle forutgående medlemskap begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Forutgaaende_medl_begrunnelser::class)


        VilkaarbegrunnelseFactory.mapArt121ForutgaaendeBegrunnelseType(begrunnelser)
    }

    @Test
    fun `mapArt121VesentligVirksomhetBegrunnelse skal mappe alle vesentlig virksomhet begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Vesentlig_virksomhet_begrunnelser::class)


        VilkaarbegrunnelseFactory.mapArt121VesentligVirksomhetBegrunnelse(begrunnelser)
    }

    @Test
    fun `mapArt122Begrunnelser skal mappe alle utsendt næringsdrivende begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Utsendt_naeringsdrivende_begrunnelser::class)


        VilkaarbegrunnelseFactory.mapArt122BegrunnelseType(begrunnelser)
    }

    @Test
    fun `mapArt122NormaltDriverVirksomhetBegrunnelser skal mappe alle normalt virksomhet begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Normalt_virksomhet_begrunnelser::class)


        VilkaarbegrunnelseFactory.mapArt122NormalVirksomhetBegrunnelseType(begrunnelser)
    }

    @Test
    fun `mapArt161AnmodningBegrunnelser skal mappe alle anmodning begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Anmodning_begrunnelser::class)


        for (begrunnelse in begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningBegrunnelser(setOf(begrunnelse))
        }
    }

    @Test
    fun `mapArt161AnmodningUtenArt12Begrunnelser skal mappe alle direkte til anmodning begrunnelser`() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Direkte_til_anmodning_begrunnelser::class)


        for (begrunnelse in begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningUtenArt12Begrunnelser(setOf(begrunnelse))
        }
    }

    fun lagAlleVilkaarBegrunnelser(kodeverk: KClass<*>): Set<VilkaarBegrunnelse> = hentAlleVerdierFraKodeverk(kodeverk)
        .map { k ->
            VilkaarBegrunnelse().apply {
                kode = k
            }
        }.toSet()
}
