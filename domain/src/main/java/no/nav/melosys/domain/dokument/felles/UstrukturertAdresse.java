package no.nav.melosys.domain.dokument.felles;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.util.AdresseUtils.sammenslå;

public class UstrukturertAdresse extends Adresse {
    private final List<String> adresselinjer = new ArrayList<>();

    private UstrukturertAdresse(String l1, String l2, String l3, String l4, String landKode) {
        if (StringUtils.isNotEmpty(l1)) {
            adresselinjer.add(l1);
        }
        if (StringUtils.isNotEmpty(l2)) {
            adresselinjer.add(l2);
        }
        if (StringUtils.isNotEmpty(l3)) {
            adresselinjer.add(l3);
        }
        if (StringUtils.isNotEmpty(l4)) {
            adresselinjer.add(l4);
        }
        if (StringUtils.isNotEmpty(landKode)) {
            this.landkode = landKode;
        }
    }

    public static UstrukturertAdresse av(MidlertidigPostadresseUtland adresse) {
        return new UstrukturertAdresse(adresse.adresselinje1, adresse.adresselinje2,
                                       adresse.adresselinje3, adresse.adresselinje4,
                                       adresse.land.getKode());
    }

    public static UstrukturertAdresse av(SemistrukturertAdresse sAdresse) {
        String poststed;
        if (sAdresse.erUtenlandsk()) {
            poststed = sAdresse.getPoststedUtland();
        } else {
            String _poststed = sAdresse.getPoststed() == null ? "" : " " + sAdresse.getPoststed();
            poststed = sAdresse.getPostnr() + _poststed;
        }

        return new UstrukturertAdresse(sAdresse.getAdresselinje1(), sAdresse.getAdresselinje2(),
                                       sAdresse.getAdresselinje3(), poststed,
                                       sAdresse.getLandkode());
    }

    public static UstrukturertAdresse av(no.nav.melosys.domain.dokument.person.UstrukturertAdresse adresse) {
        return new UstrukturertAdresse(adresse.adresselinje1, adresse.adresselinje2,
                                       adresse.adresselinje3, adresse.adresselinje4,
                                       adresse.land.getKode());
    }

    public static UstrukturertAdresse av(StrukturertAdresse sAdresse) {
        String linje1 = sammenslå(sAdresse.gatenavn, sAdresse.husnummer);

        return new UstrukturertAdresse(linje1,
                                       sAdresse.postnummer,
                                       sAdresse.poststed,
                                       sAdresse.region,
                                       sAdresse.landkode);
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    @JsonIgnore
    public String getAdresselinje(int linjenummer) {
        if (linjenummer > adresselinjer.size()) {
            return null;
        } else {
            return adresselinjer.get(linjenummer - 1);
        }
    }

    public boolean erTom() {
        return adresselinjer.isEmpty() && StringUtils.isEmpty(landkode);
    }
}
