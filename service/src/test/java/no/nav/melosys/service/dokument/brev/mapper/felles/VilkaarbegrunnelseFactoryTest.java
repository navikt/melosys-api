package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.*;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk;

public class VilkaarbegrunnelseFactoryTest {

    @Test
    public void mapArt121BegrunnelseType() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art12_1_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt121BegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt121ForugåendeBegrunnelse() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art12_1_forutgaaende_medl.class);
        VilkaarbegrunnelseFactory.mapArt121ForutgaaendeBegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt121VesentligVirksomhetBegrunnelse() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art12_1_vesentlig_virksomhet.class);
        VilkaarbegrunnelseFactory.mapArt121VesentligVirksomhetBegrunnelse(begrunnelser);
    }

    @Test
    public void mapArt122Begrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art12_2_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt122BegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt122NormaltDriverVirksomhetBegrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art12_2_normalt_virksomhet.class);
        VilkaarbegrunnelseFactory.mapArt122NormalVirksomhetBegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt161AnmodningBegrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art16_1_anmodning.class);
        for (VilkaarBegrunnelse begrunnelse : begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningBegrunnelser(Collections.singleton(begrunnelse));
        }
    }

    @Test
    public void mapArt161AnmodningUtenArt12Begrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art16_1_anmodning_uten_art12.class);
        for (VilkaarBegrunnelse begrunnelse : begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningUtenArt12Begrunnelser(Collections.singleton(begrunnelse));
        }
    }

    public static Set<VilkaarBegrunnelse> lagAlleVilkaarBegrunnelser(Class kodeverk) throws Exception {
        return hentAlleVerdierFraKodeverk(kodeverk)
            .map(k -> {
                VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
                vilkaarBegrunnelse.setKode(k);
                return vilkaarBegrunnelse;
            })
            .collect(Collectors.toSet());
    }
}
