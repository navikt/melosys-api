package no.nav.melosys.service.dokument.sed.mapper;

import java.util.*;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class VilkaarsresultatTilBeskrivelseMapperTest {

    @Test
    public void mapVilkaarsresultatTilBeskrivelseString_forventSøktForSentBeskrivelse() {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("SOEKT_FOR_SENT");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelse);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);

        assertThat(VilkaarsresultatTilBeskrivelseMapper.mapVilkaarsresultatTilBeskrivelseString(vilkaarsresultat),
                is("Søkt mer enn ett år etter perioden startet"));
    }

    @Test
    public void mapVilkaarsresultatTilBeskrivelseString_forventFritekst() {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("SAERLIG_AVSLAGSGRUNN");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelse);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som beskriver avslagsgrunn");

        assertThat(VilkaarsresultatTilBeskrivelseMapper.mapVilkaarsresultatTilBeskrivelseString(vilkaarsresultat),
            is("Fritekst som beskriver avslagsgrunn"));
    }

    @Test
    public void mapVilkaarsresultatTilBeskrivelseString_medFlereBegrunnelser_forventSammensattBegrunnelse() {
        VilkaarBegrunnelse vilkaarBegrunnelseForSent = new VilkaarBegrunnelse();
        vilkaarBegrunnelseForSent.setKode("SOEKT_FOR_SENT");
        VilkaarBegrunnelse vilkaarBegrunnelseErstatter = new VilkaarBegrunnelse();
        vilkaarBegrunnelseErstatter.setKode("ERSTATTER_EN_ANNEN_SAMLET_OVER_5_AAR");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelseForSent);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseErstatter);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som ikke skal vises");

        assertThat(VilkaarsresultatTilBeskrivelseMapper.mapVilkaarsresultatTilBeskrivelseString(vilkaarsresultat),
            is("Erstatter en annen utsendt person, samlet periode mer enn 5 år\nSøkt mer enn ett år etter perioden startet"));
    }

    @Test
    public void mapVilkaarsresultatTilBeskrivelseString_medFlereBegrunnelserOgFritekst_forventFritekst() {
        VilkaarBegrunnelse vilkaarBegrunnelseForSent = new VilkaarBegrunnelse();
        vilkaarBegrunnelseForSent.setKode("SOEKT_FOR_SENT");
        VilkaarBegrunnelse vilkaarBegrunnelseErstatter = new VilkaarBegrunnelse();
        vilkaarBegrunnelseErstatter.setKode("ERSTATTER_EN_ANNEN_SAMLET_OVER_5_AAR");
        VilkaarBegrunnelse vilkaarBegrunnelseSærlig = new VilkaarBegrunnelse();
        vilkaarBegrunnelseSærlig.setKode("SAERLIG_AVSLAGSGRUNN");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelseForSent);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseErstatter);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseSærlig);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som skal vises");

        assertThat(VilkaarsresultatTilBeskrivelseMapper.mapVilkaarsresultatTilBeskrivelseString(vilkaarsresultat),
            is("Fritekst som skal vises"));
    }
}