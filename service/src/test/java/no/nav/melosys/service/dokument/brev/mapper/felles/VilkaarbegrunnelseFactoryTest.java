package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.*;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk;

public class VilkaarbegrunnelseFactoryTest {

    @Test
    public void mapArt121BegrunnelseType() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Utsendt_arbeidstaker_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt121BegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt121ForugåendeBegrunnelse() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Forutgaaende_medl_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt121ForutgaaendeBegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt121VesentligVirksomhetBegrunnelse() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Vesentlig_virksomhet_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt121VesentligVirksomhetBegrunnelse(begrunnelser);
    }

    @Test
    public void mapArt122Begrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Utsendt_naeringsdrivende_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt122BegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt122NormaltDriverVirksomhetBegrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Normalt_virksomhet_begrunnelser.class);
        VilkaarbegrunnelseFactory.mapArt122NormalVirksomhetBegrunnelseType(begrunnelser);
    }

    @Test
    public void mapArt161AnmodningBegrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Anmodning_begrunnelser.class);
        for (VilkaarBegrunnelse begrunnelse : begrunnelser) {
            VilkaarbegrunnelseFactory.mapAnmodningBegrunnelser(Collections.singleton(begrunnelse));
        }
    }

    @Test
    public void mapArt161AnmodningUtenArt12Begrunnelser() throws Exception {
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Direkte_til_anmodning_begrunnelser.class);
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
