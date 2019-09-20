package no.nav.melosys.tjenester.gui.dto.periode;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto.enumVerdiEllerNull;

public class LovvalgsperiodeDtoTest {

    private static final String JSON_MAL = "" +
            "{" +
            "  \"fomDato\": \"2019-01-01\"," +
            "  \"tomDato\": \"2020-01-01\"," +
            "  \"lovvalgsbestemmelse\": \"FO_883_2004_ART12_1\"," +
            "  \"tilleggBestemmelse\": \"FO_883_2004_ART11_2\"," +
            "  \"unntakFraBestemmelse\": %s," +
            "  \"innvilgelsesResultat\": \"INNVILGET\"," +
            "  \"lovvalgsland\": \"NO\"," +
            "  \"unntakFraLovvalgsland\": %s," +
            "  \"trygdeDekning\": %s," +
            "  \"medlemskapstype\": \"PLIKTIG\"," +
            "  \"medlemskapsperiodeID\": 20" +
            "}";

    private static final String JSON_EKSEMPEL = String.format(JSON_MAL, "\"FO_883_2004_ART11_1\"", "\"NO\"", "\"FULL_DEKNING_EOSFO\"");

    @Test
    public void mapKonstruktørLagerSammeObjektSomOrdinærKonstruktør() throws Exception {
        Map<String, String> json = new ObjectMapper().readValue(JSON_EKSEMPEL, new TypeReference<Map<String, String>>() {
        });
        LovvalgsperiodeDto resultat = new LovvalgsperiodeDto(json);
        LovvalgsperiodeDto forventet = lagLovvalgsperiodeDtoFraMap(json);
        Assertions.assertThat(resultat).isEqualToComparingFieldByFieldRecursively(forventet);
    }

    @Test
    public void mapKonstruktørLagerSammeObjektSomOrdinærKonstruktørUtenLandkodeUtenMedlemskapstypeOgLovvalgsbestemmelse() throws Exception {
        Map<String, String> json = new ObjectMapper().readValue(JSON_EKSEMPEL, new TypeReference<Map<String, String>>() {
        });
        json.remove("lovvalgsland");
        json.remove("medlemskapstype");
        json.remove("lovvalgsbestemmelse");

        LovvalgsperiodeDto resultat = new LovvalgsperiodeDto(json);
        LovvalgsperiodeDto forventet = lagLovvalgsperiodeDtoFraMap(json);
        Assertions.assertThat(resultat).isEqualToComparingFieldByFieldRecursively(forventet);
    }

    private static LovvalgsperiodeDto lagLovvalgsperiodeDtoFraMap(Map<String, String> json) {
        LovvalgsperiodeDto forventet = new LovvalgsperiodeDto(
            new PeriodeDto(LocalDate.parse(json.get("fomDato")), LocalDate.parse(json.get("tomDato"))),
            enumVerdiEllerNull(Lovvalgbestemmelser_883_2004.class, json.get("lovvalgsbestemmelse")),
            Tilleggsbestemmelser_883_2004.valueOf(json.get("tilleggBestemmelse")),
            enumVerdiEllerNull(Landkoder.class, json.get("lovvalgsland")),
            InnvilgelsesResultat.valueOf(json.get("innvilgelsesResultat")),
            enumVerdiEllerNull(Trygdedekninger.class, json.get("trygdeDekning")),
            enumVerdiEllerNull(Medlemskapstyper.class, json.get("medlemskapstype")),
            "20");
        return forventet;
    }

}
