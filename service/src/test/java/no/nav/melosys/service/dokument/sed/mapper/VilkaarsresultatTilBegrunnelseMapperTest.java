package no.nav.melosys.service.dokument.sed.mapper;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VilkaarsresultatTilBegrunnelseMapperTest {

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_medArt12_1_forventUtsendelseOver24MndBeskrivelse() {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("UTSENDELSE_OVER_24_MN");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelse);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
            is("Utsendelseperioden overskrider 24 måneder"));
    }

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_medArt16_1_forventUtsendelseMellom24MndOg5AarBeskrivelse() {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("UTSENDELSE_MELLOM_24_MN_OG_5_AAR");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelse);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
                is("Utsendelseperioden er mellom 2 og 5 år"));
    }

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_forventFritekst() {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("SAERLIG_GRUNN");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelse);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som beskriver anmodning om unntak");

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
            is("Fritekst som beskriver anmodning om unntak"));
    }

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_medFlereBegrunnelser_forventSammensattBeskrivelse() {
        VilkaarBegrunnelse vilkaarBegrunnelseUtsendelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelseUtsendelse.setKode("UTSENDELSE_OVER_24_MN");
        VilkaarBegrunnelse vilkaarBegrunnelseIdeellOrganisasjon = new VilkaarBegrunnelse();
        vilkaarBegrunnelseIdeellOrganisasjon.setKode("IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelseUtsendelse);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseIdeellOrganisasjon);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som ikke skal vises");

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
            is("Utsendelseperioden overskrider 24 måneder\nArbeider for ideell organisasjon som ikke har vesentlig virksomhet i Norge"));
    }

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_medFlereBegrunnelserOgFritekst_forventSammensattBeskrivelseOgFritekst() {
        VilkaarBegrunnelse vilkaarBegrunnelseUtsendelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelseUtsendelse.setKode("UTSENDELSE_OVER_24_MN");
        VilkaarBegrunnelse vilkaarBegrunnelseIdeellOrganisasjon = new VilkaarBegrunnelse();
        vilkaarBegrunnelseIdeellOrganisasjon.setKode("IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK");
        VilkaarBegrunnelse vilkaarBegrunnelseSærlig = new VilkaarBegrunnelse();
        vilkaarBegrunnelseSærlig.setKode("SAERLIG_GRUNN");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelseUtsendelse);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseIdeellOrganisasjon);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseSærlig);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som skal vises");

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
            is("Utsendelseperioden overskrider 24 måneder\nFritekst som skal vises\nArbeider for ideell organisasjon som ikke har vesentlig virksomhet i Norge"));
    }

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_medKodeSomIkkeFinnes_forventTomString() {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("EN_KODE_SOM_IKKE_FINNES_I_KODEVERK");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelse);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
            is(""));
    }

    @Test
    public void mapVilkaarsresultatTilBegrunnelseString_medKodeSomIkkeFinnesOgAndreBegrunnelser_forventBeskrivelser() {
        VilkaarBegrunnelse vilkaarBegrunnelseUtsendelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelseUtsendelse.setKode("UTSENDELSE_OVER_24_MN");
        VilkaarBegrunnelse vilkaarBegrunnelseFinnesIkke = new VilkaarBegrunnelse();
        vilkaarBegrunnelseFinnesIkke.setKode("EN_KODE_SOM_IKKE_FINNES_I_KODEVERK");
        VilkaarBegrunnelse vilkaarBegrunnelseIdeellOrganisasjon = new VilkaarBegrunnelse();
        vilkaarBegrunnelseIdeellOrganisasjon.setKode("IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK");

        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();
        vilkaarBegrunnelser.add(vilkaarBegrunnelseUtsendelse);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseFinnesIkke);
        vilkaarBegrunnelser.add(vilkaarBegrunnelseIdeellOrganisasjon);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);

        assertThat(VilkaarsresultatTilBegrunnelseMapper.mapVilkaarsresultatTilBegrunnelseString(vilkaarsresultat),
            is("Utsendelseperioden overskrider 24 måneder\nArbeider for ideell organisasjon som ikke har vesentlig virksomhet i Norge"));
    }
}