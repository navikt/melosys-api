package no.nav.melosys.service.dokument.brev.mapper.felles

import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.kodeverk.begrunnelser.*
import no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk
import org.junit.jupiter.api.Test
import java.util.stream.Collectors

class VilkaarbegrunnelseFactoryKtTest {

    @Test
    fun mapArt121BegrunnelseType() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Utsendt_arbeidstaker_begrunnelser::class.java)
        VilkaarbegrunnelseFactory.mapArt121BegrunnelseType(begrunnelser)
    }

    @Test
    fun mapArt121ForugåendeBegrunnelse() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Forutgaaende_medl_begrunnelser::class.java)
        VilkaarbegrunnelseFactory.mapArt121ForutgaaendeBegrunnelseType(begrunnelser)
    }

    @Test
    fun mapArt121VesentligVirksomhetBegrunnelse() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Vesentlig_virksomhet_begrunnelser::class.java)
        VilkaarbegrunnelseFactory.mapArt121VesentligVirksomhetBegrunnelse(begrunnelser)
    }

    @Test
    fun mapArt122Begrunnelser() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Utsendt_naeringsdrivende_begrunnelser::class.java)
        VilkaarbegrunnelseFactory.mapArt122BegrunnelseType(begrunnelser)
    }

    @Test
    fun mapArt122NormaltDriverVirksomhetBegrunnelser() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Normalt_virksomhet_begrunnelser::class.java)
        VilkaarbegrunnelseFactory.mapArt122NormalVirksomhetBegrunnelseType(begrunnelser)
    }

    @Test
    fun mapArt161AnmodningBegrunnelser() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Anmodning_begrunnelser::class.java)
        for (begrunnelse in begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningBegrunnelser(setOf(begrunnelse))
        }
    }

    @Test
    fun mapArt161AnmodningUtenArt12Begrunnelser() {
        val begrunnelser = lagAlleVilkaarBegrunnelser(Direkte_til_anmodning_begrunnelser::class.java)
        for (begrunnelse in begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningUtenArt12Begrunnelser(setOf(begrunnelse))
        }
    }

    companion object {
        fun lagAlleVilkaarBegrunnelser(kodeverk: Class<*>): Set<VilkaarBegrunnelse> {
            return hentAlleVerdierFraKodeverk(kodeverk)
                .map { k ->
                    VilkaarBegrunnelse().apply {
                        kode = k
                    }
                }
                .collect(Collectors.toSet())
        }
    }
}
