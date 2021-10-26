package no.nav.melosys.integrasjon.dokgen;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

import static org.springframework.util.StringUtils.hasText;

public final class DokgenAdresseMapper {

    private DokgenAdresseMapper() {
    }

    public static String mapMottakerNavn(OrganisasjonDokument org, Persondata persondata) {
        return org == null ? persondata.getSammensattNavn() : org.getNavn();
    }

    public static List<String> mapAdresselinjer(OrganisasjonDokument org, String kontaktperson,
                                                Kontaktopplysning kontaktopplysning, Persondata persondata) {
        List<String> adresselinjer;
        if (org == null) {
            adresselinjer = persondata.hentGjeldendePostadresse().adresselinjer();
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            adresselinjer = new ArrayList<>();
            if (hasText(kontaktperson)) {
                adresselinjer.add("Att: " + kontaktperson);
            } else if (kontaktopplysning != null && hasText(kontaktopplysning.getKontaktNavn())) {
                adresselinjer.add("Att: " + kontaktopplysning.getKontaktNavn());
            }

            adresselinjer.add(orgAdresse.getGatenavn() +
                ((orgAdresse.getHusnummerEtasjeLeilighet() == null) ? "" : " " + orgAdresse.getHusnummerEtasjeLeilighet()));
        }
        return adresselinjer;
    }

    public static String mapPostnr(OrganisasjonDokument org, Persondata persondata) {
        String postNr;
        if (org == null) {
            postNr = persondata.hentGjeldendePostadresse().postnr();
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            postNr = orgAdresse.getPostnummer();
        }
        return postNr;
    }

    public static String mapPoststed(OrganisasjonDokument org) {
        return mapPoststed(org, null);
    }

    public static String mapPoststed(OrganisasjonDokument org, Persondata persondata) {
        String poststed;
        if (org == null) {
            poststed = persondata.hentGjeldendePostadresse().poststed();
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            poststed = orgAdresse.getPoststed();
        }
        return poststed;
    }

    public static String mapLandForAdresse(OrganisasjonDokument org, Persondata persondata) {
        String landkode;
        if (org == null) {
            landkode = persondata.hentGjeldendePostadresse().landkode();
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            landkode = orgAdresse.getLandkode() != null ? orgAdresse.getLandkode() : null;
        }
        return landkode;
    }

    public static Mottaker mapMottaker(OrganisasjonDokument org, String kontaktperson,
                                       Kontaktopplysning kontaktopplysning, Persondata persondata) {
        return new Mottaker(
            mapMottakerNavn(org, persondata),
            mapAdresselinjer(org, kontaktperson, kontaktopplysning, persondata),
            mapPostnr(org, persondata),
            mapPoststed(org, persondata),
            mapLandForAdresse(org, persondata)
        );
    }
}
