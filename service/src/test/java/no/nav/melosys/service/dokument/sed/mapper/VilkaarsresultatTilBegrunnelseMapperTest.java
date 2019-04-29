package no.nav.melosys.service.dokument.sed.mapper;

import java.util.*;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class VilkaarsresultatTilBegrunnelseMapperTest {

    @Test
    public void tilEngelskBegrunnelseString_medArt16_1_forventBeskrivelse() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Collections.singletonList("UTSENDELSE_MELLOM_24_MN_OG_5_AAR"));

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Ongoing contract for a period longer than 2 and shorter than 5 years.");
    }

    @Test
    public void tilEngelskBegrunnelseString_medFlereBegrunnelser_forventSammensattBeskrivelse() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Arrays.asList(
            "UTSENDELSE_MELLOM_24_MN_OG_5_AAR",
            "IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK"
        ));

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Ongoing contract for a period longer than 2 and shorter than 5 years.\n"
                + "Working for a non-profit organisation, but the Norwegian organisation does not normally carry out its activity in Norway.");
    }

    @Test
    public void tilEngelskBegrunnelseString_forventFritekst() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Collections.singletonList("SAERLIG_GRUNN"));
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som beskriver anmodning om unntak");

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Fritekst som beskriver anmodning om unntak");
    }

    @Test
    public void tilEngelskBegrunnelseString_medFlereBegrunnelserOgFritekst_forventSammensattBeskrivelseOgFritekst() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Arrays.asList(
            "UTSENDELSE_MELLOM_24_MN_OG_5_AAR",
            "IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK",
            "SAERLIG_GRUNN"
        ));
        vilkaarsresultat.setBegrunnelseFritekst("Fritekst som skal vises");

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Ongoing contract for a period longer than 2 and shorter than 5 years.\n"
                + "Fritekst som skal vises\n"
                + "Working for a non-profit organisation, but the Norwegian organisation does not normally carry out its activity in Norway.");
    }

    @Test
    public void tilEngelskBegrunnelseString_medKodeSomIkkeFinnes_forventTomString() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Collections.singletonList("EN_KODE_SOM_IKKE_FINNES_I_KODEVERK"));

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("");
    }

    private Vilkaarsresultat lagVilkaarsresultatMedBegrunnelser(List<String> vilkaarBegrunnelseKoder) {
        Set<VilkaarBegrunnelse> vilkaarBegrunnelser = new HashSet<>();

        vilkaarBegrunnelseKoder.stream()
            .map(this::lagVilkaarBegrunnelse)
            .forEach(vilkaarBegrunnelser::add);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(vilkaarBegrunnelser);

        return vilkaarsresultat;
    }

    private VilkaarBegrunnelse lagVilkaarBegrunnelse(String kode) {
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode(kode);
        return vilkaarBegrunnelse;
    }
}