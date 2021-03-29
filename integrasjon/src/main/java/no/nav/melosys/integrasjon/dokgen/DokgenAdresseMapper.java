package no.nav.melosys.integrasjon.dokgen;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import static org.springframework.util.StringUtils.hasText;

public final class DokgenAdresseMapper {

    private DokgenAdresseMapper() {
    }

    public static String mapMottakerNavn(OrganisasjonDokument org, PersonDokument personDokument) {
        return org == null ? personDokument.sammensattNavn : org.getNavn();
    }

    public static List<String> mapAdresselinjer(OrganisasjonDokument org, String kontaktperson, Kontaktopplysning kontaktopplysning, PersonDokument personDokument) {
        List<String> adresselinjer;
        if (org == null) {
            adresselinjer = personDokument.gjeldendePostadresse.adresselinjer();
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            adresselinjer = new ArrayList<>();
            if (hasText(kontaktperson)) {
                adresselinjer.add("Att: " + kontaktperson);
            } else if (kontaktopplysning != null && hasText(kontaktopplysning.getKontaktNavn())) {
                adresselinjer.add("Att: " + kontaktopplysning.getKontaktNavn());
            }

            adresselinjer.add(orgAdresse.gatenavn +
                ((orgAdresse.husnummer == null) ? "" : " " + orgAdresse.husnummer));
        }
        return adresselinjer;
    }

    public static String mapPostnr(OrganisasjonDokument org, PersonDokument personDokument) {
        String postNr;
        if (org == null) {
            postNr = personDokument.gjeldendePostadresse.postnr;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            postNr = orgAdresse.postnummer;
        }
        return postNr;
    }

    public static String mapPoststed(OrganisasjonDokument org) {
        return mapPoststed(org, null);
    }

    public static String mapPoststed(OrganisasjonDokument org, PersonDokument personDokument) {
        String poststed;
        if (org == null) {
            poststed = personDokument.gjeldendePostadresse.poststed;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            poststed = orgAdresse.poststed;
        }
        return poststed;
    }

    public static String mapLandForAdresse(OrganisasjonDokument org, PersonDokument personDokument) {
        String land;
        if (org == null) {
            land = personDokument.gjeldendePostadresse.land != null ? personDokument.gjeldendePostadresse.land.toString() : null;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            land = orgAdresse.landkode != null ? orgAdresse.landkode : null;
        }
        return land;
    }

    private static StrukturertAdresse hentTilgjengeligAdresse(OrganisasjonDokument org) {
        return org.getPostadresse() == null ? org.getForretningsadresse() : org.getPostadresse();
    }
}
