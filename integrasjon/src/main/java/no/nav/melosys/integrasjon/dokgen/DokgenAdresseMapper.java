package no.nav.melosys.integrasjon.dokgen;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import static no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument.hentTilgjengeligAdresse;
import static org.springframework.util.StringUtils.hasText;

public final class DokgenAdresseMapper {

    private DokgenAdresseMapper(){}

    public static String mapMottakerNavn(OrganisasjonDokument org, PersonDokument personDokument) {
        return org == null ? personDokument.getSammensattNavn() : org.getNavn();
    }

    public static List<String> mapAdresselinjer(OrganisasjonDokument org, String kontaktperson, Kontaktopplysning kontaktopplysning, PersonDokument personDokument) {
        List<String> adresselinjer;
        if (org == null) {
            adresselinjer = personDokument.getGjeldendePostadresse().adresselinjer();
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
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

    public static String mapPostnr(OrganisasjonDokument org, PersonDokument personDokument) {
        String postNr;
        if (org == null) {
            postNr = personDokument.getGjeldendePostadresse().postnr;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            postNr = orgAdresse.getPostnummer();
        }
        return postNr;
    }

    public static String mapPoststed(OrganisasjonDokument org) {
        return mapPoststed(org, null);
    }

    public static String mapPoststed(OrganisasjonDokument org, PersonDokument personDokument) {
        String poststed;
        if (org == null) {
            poststed = personDokument.getGjeldendePostadresse().poststed;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            poststed = orgAdresse.getPoststed();
        }
        return poststed;
    }

    public static String mapLandForAdresse(OrganisasjonDokument org, PersonDokument personDokument) {
        String land;
        if (org == null) {
            land = personDokument.getGjeldendePostadresse().land != null ? personDokument.getGjeldendePostadresse().land.toString() : null;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            land = orgAdresse.getLandkode() != null ? orgAdresse.getLandkode() : null;
        }
        return land;
    }
}
