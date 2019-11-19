package no.nav.melosys.domain.dokument.adresse;

import java.util.Objects;

import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.util.LandkoderUtils;
import org.apache.commons.lang3.StringUtils;

public class StrukturertAdresse extends Adresse {
    public String gatenavn;
    // Sammensatt av husnummer og husbokstav
    public String husnummer;
    public String region;
    public String postnummer;
    public String poststed;

    public static StrukturertAdresse av(Bostedsadresse bostedsadresse) {
        Gateadresse gateadresse = bostedsadresse.getGateadresse();
        StrukturertAdresse adresse = new StrukturertAdresse();
        if (gateadresse != null) {
            adresse.gatenavn = gateadresse.getGatenavn();
            adresse.husnummer = Objects.toString(gateadresse.getHusnummer(), "");
            adresse.husnummer += Objects.toString(gateadresse.getHusbokstav(), "");
        }

        adresse.postnummer = bostedsadresse.getPostnr();
        adresse.poststed = bostedsadresse.getPoststed();
        if (StringUtils.isNotEmpty(bostedsadresse.getLand().getKode())) {
            adresse.landkode = LandkoderUtils.tilIso2(bostedsadresse.getLand().getKode());
        }

        return adresse;
    }

    @Override
    public boolean erTom() {
        return StringUtils.isAllEmpty(gatenavn, husnummer, postnummer, poststed, region, landkode);
    }
}