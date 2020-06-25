package no.nav.melosys.service.dokument.sed.mapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_engelsk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12_engelsk;
import org.junit.Test;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning.SAERLIG_GRUNN;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning.UTSENDELSE_MELLOM_24_MN_OG_5_AAR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12.SJOEMANNSKIRKEN;
import static no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk;
import static org.assertj.core.api.Assertions.assertThat;

public class VilkaarsresultatTilBegrunnelseMapperTest {

    @Test
    public void tilEngelskBegrunnelseString_medArt16_1_forventBeskrivelse() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Collections.singletonList(UTSENDELSE_MELLOM_24_MN_OG_5_AAR.getKode()));

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Ongoing contract for a period longer than 2 and shorter than 5 years.");
    }

    @Test
    public void tilEngelskBegrunnelseString_medArt16UtenArt12_forventBeskrivelse() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Collections.singletonList(SJOEMANNSKIRKEN.getKode()));

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Working for Sjømannskirken (The Norwegian Seamen’s Church), which is a nonprofit organization receiving financial support from the Norwegian Government.");
    }

    @Test
    public void testArt161Anmodning_motArt161AnmodningEngelsk() throws Exception {
        Stream<String> begrunnelserArt16 = hentAlleVerdierFraKodeverk(Art16_1_anmodning.class);
        Stream<String> begrunnelserArt16Engelsk = hentAlleVerdierFraKodeverk(Art16_1_anmodning_engelsk.class);

        assertThat(begrunnelserArt16).containsExactlyElementsOf(begrunnelserArt16Engelsk.collect(Collectors.toList()));
    }

    @Test
    public void testArt16AnmodningUtenArt12_motArt16AnmodningUtenArt12Engelsk() throws Exception {
        Stream<String> begrunnelserArt16Uten12 = hentAlleVerdierFraKodeverk(Art16_1_anmodning_uten_art12.class);
        Stream<String> begrunnelserArt16Uten12Engelsk = hentAlleVerdierFraKodeverk(Art16_1_anmodning_uten_art12_engelsk.class);

        assertThat(begrunnelserArt16Uten12).containsExactlyElementsOf(begrunnelserArt16Uten12Engelsk.collect(Collectors.toList()));
    }

    @Test
    public void testArt161anmodning_motArt161AnmodningUtenArt12Engelsk() throws Exception {
        Set<String> begrunnelserArt16Engelsk = hentAlleVerdierFraKodeverk(Art16_1_anmodning_engelsk.class).collect(Collectors.toSet());
        Set<String> begrunnelserArt16Uten12Engelsk = hentAlleVerdierFraKodeverk(Art16_1_anmodning_uten_art12_engelsk.class).collect(Collectors.toSet());

        // Ok å ha samme kode i begge listene, så lenge den engelske beskrivelsen også er lik
        Set<String> koderTilstedeIBeggeLister = new HashSet<>(begrunnelserArt16Engelsk);
        koderTilstedeIBeggeLister.retainAll(begrunnelserArt16Uten12Engelsk);
        koderTilstedeIBeggeLister.remove(SAERLIG_GRUNN.getKode());

        for (String kode : koderTilstedeIBeggeLister) {
            String art16Beskrivelse_engelsk = Art16_1_anmodning_engelsk.valueOf(kode).getBeskrivelse();
            String art16UtenArt12Beskrivelse_engelsk = Art16_1_anmodning_uten_art12_engelsk.valueOf(kode).getBeskrivelse();

            assertThat(art16Beskrivelse_engelsk).isEqualTo(art16UtenArt12Beskrivelse_engelsk);
        }
    }

    @Test
    public void tilEngelskBegrunnelseString_Art16MedFlereBegrunnelser_forventSammensattBeskrivelse() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Arrays.asList(
            "UTSENDELSE_MELLOM_24_MN_OG_5_AAR",
            "IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK"
        ));

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Ongoing contract for a period longer than 2 and shorter than 5 years.\n"
                + "Working for a non-profit organization.");
    }

    @Test
    public void tilEngelskBegrunnelseString_art16_forventFritekst() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Collections.singletonList("SAERLIG_GRUNN"));
        vilkaarsresultat.setBegrunnelseFritekstEessi("Fritekst som beskriver anmodning om unntak");

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Fritekst som beskriver anmodning om unntak");
    }

    @Test
    public void tilEngelskBegrunnelseString_Art16MedFlereBegrunnelserOgFritekst_forventSammensattBeskrivelseOgFritekst() {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultatMedBegrunnelser(Arrays.asList(
            "UTSENDELSE_MELLOM_24_MN_OG_5_AAR",
            "IDEELL_ORGANISASJON_IKKE_VESENTLIG_VIRK",
            "SAERLIG_GRUNN"
        ));
        final String fritekstEngelsk = "Something";
        vilkaarsresultat.setBegrunnelseFritekstEessi(fritekstEngelsk);

        assertThat(VilkaarsresultatTilBegrunnelseMapper.tilEngelskBegrunnelseString(vilkaarsresultat))
            .isEqualTo("Ongoing contract for a period longer than 2 and shorter than 5 years.\n"
                + fritekstEngelsk + "\n"
                + "Working for a non-profit organization.");
    }

    @Test
    public void tilEngelskBegrunnelseString_MedKodeSomIkkeFinnes_forventTomString() {
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