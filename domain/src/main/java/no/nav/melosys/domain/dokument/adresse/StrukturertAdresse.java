package no.nav.melosys.domain.dokument.adresse;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.LandkoderUtils;
import org.apache.commons.lang3.StringUtils;

public class StrukturertAdresse extends Adresse {
    public String gatenavn;
    // Sammensatt av husnummer og husbokstav
    public String husnummer;
    public String postnummer;
    public String poststed;
    public String region;

    public StrukturertAdresse() {
    }

    public StrukturertAdresse(String gatenavn,
                              String husnummer,
                              String postnummer,
                              String poststed,
                              String region,
                              String landkode) {
        this.gatenavn = gatenavn;
        this.husnummer = husnummer;
        this.region = region;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.landkode = landkode;
    }

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

    @Override
    public String toString() {
        return Stream.of(sammenslå(gatenavn, husnummer),
                region,
                postnummer, poststed,
                Landkoder.valueOf(landkode).getBeskrivelse())
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(", "));
    }
}