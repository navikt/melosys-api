package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.tjenester.gui.dto.LovvalgsperiodeDto.enumVerdiEllerNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Medlemskapstype;
import no.nav.melosys.domain.TrygdeDekning;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.tjenester.gui.dto.LovvalgsperiodeDto.LovvalgBestemmelseILand;
import no.nav.melosys.tjenester.gui.dto.LovvalgsperiodeDto.UnntakFraBestemmelseILand;

public class LovvalgsperiodeDtoTest {

    private static final String JSON_MAL = "" +
            "{" +
            "  \"fomDato\": \"2019-01-01\"," +
            "  \"tomDato\": \"2020-01-01\"," +
            "  \"lovvalgBestemmelse\": \"ART12_1\"," +
            "  \"unntakFraBestemmelse\": %s," +
            "  \"innvilgelsesResultat\": \"INNVILGET\"," +
            "  \"lovvalgsland\": \"NO\"," +
            "  \"unntakFraLovvalgsland\": %s," +
            "  \"trygdeDekning\": %s," +
            "  \"medlemskapstype\": \"PLIKTIG\"" +
            "}";

    private static final String JSON_EKSEMPEL = String.format(JSON_MAL, "\"ART12_1\"", "\"NO\"", "\"FULL_DEKNING_EOSFO\"");

    @Test
    public void mapKonstruktørLagerSammeObjektSomOrdinærKonstruktør() throws Exception {
        Map<String, String> json = new ObjectMapper().readValue(JSON_EKSEMPEL, new TypeReference<Map<String, String>>() {
        });
        LovvalgsperiodeDto resultat = new LovvalgsperiodeDto(json);
        LovvalgsperiodeDto forventet = lagLovvalgsperiodeDtoFraMap(json);
        assertThat(resultat).isEqualToComparingFieldByFieldRecursively(forventet);
    }

    private static LovvalgsperiodeDto lagLovvalgsperiodeDtoFraMap(Map<String, String> json) {
        LovvalgBestemmelseILand lovvalg = new LovvalgBestemmelseILand(LovvalgBestemmelse_883_2004.valueOf(json.get("lovvalgBestemmelse")),
                Landkoder.valueOf(json.get("lovvalgsland")));
        UnntakFraBestemmelseILand unntak = new UnntakFraBestemmelseILand(enumVerdiEllerNull(LovvalgBestemmelse_883_2004.class, json.get("unntakFraBestemmelse")),
                enumVerdiEllerNull(Landkoder.class, json.get("unntakFraLovvalgsland")));
        LovvalgsperiodeDto forventet = new LovvalgsperiodeDto(new PeriodeDto(LocalDate.parse(json.get("fomDato")), LocalDate.parse(json.get("tomDato"))),
                lovvalg, unntak, InnvilgelsesResultat.valueOf(json.get("innvilgelsesResultat")),
                enumVerdiEllerNull(TrygdeDekning.class, json.get("trygdeDekning")), Medlemskapstype.valueOf(json.get("medlemskapstype")));
        return forventet;
    }

}
