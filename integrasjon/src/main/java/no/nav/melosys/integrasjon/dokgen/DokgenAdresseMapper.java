package no.nav.melosys.integrasjon.dokgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

import static org.springframework.util.StringUtils.hasText;

public final class DokgenAdresseMapper {

    private DokgenAdresseMapper() {
    }

    public static String mapNavn(OrganisasjonDokument org, Persondata persondata) {
        return org == null ? persondata.getSammensattNavn() : org.getNavn();
    }

    public static List<String> mapAdresselinjer(OrganisasjonDokument org, String kontaktperson,
                                                Kontaktopplysning kontaktopplysning, Persondata persondata) {
        List<String> adresselinjer;
        if (org == null) {
            Postadresse postadresse = persondata.hentGjeldendePostadresse();
            adresselinjer = postadresse != null ? postadresse.adresselinjer() : null;
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
            Postadresse postadresse = persondata.hentGjeldendePostadresse();
            postNr = postadresse != null ? postadresse.postnr() : null;
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
            Postadresse postadresse = persondata.hentGjeldendePostadresse();
            poststed = postadresse != null ? postadresse.poststed() : null;
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            poststed = orgAdresse.getPoststed();
        }
        return poststed;
    }

    public static String mapLandForAdresse(OrganisasjonDokument org, Persondata persondata) {
        String landkode;
        if (org == null) {
            Postadresse postadresse = persondata.hentGjeldendePostadresse();
            landkode = postadresse != null ? postadresse.landkode() : null;
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            landkode = orgAdresse.getLandkode() != null ? orgAdresse.getLandkode() : null;
        }
        return landkode;
    }

    public static String mapRegionForAdresse(OrganisasjonDokument org, Persondata persondata) {
        String region;
        if(org == null) {
            Postadresse postadresse = persondata.hentGjeldendePostadresse();
            region = postadresse != null ? postadresse.region() : null;
        } else {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            region = orgAdresse.getRegion() != null ? orgAdresse.getRegion() : null;
        }
        return region;
    }

    public static Mottaker mapMottaker(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        if (mottakerType == Aktoersroller.TRYGDEMYNDIGHET && brevbestilling.getUtenlandskMyndighet() != null) {
            var utenlandskMyndighet = brevbestilling.getUtenlandskMyndighet();
            return new Mottaker(
                utenlandskMyndighet.navn,
                Collections.singletonList(utenlandskMyndighet.gateadresse),
                utenlandskMyndighet.postnummer,
                utenlandskMyndighet.poststed,
                utenlandskMyndighet.land,
                mottakerType.getKode(),
                null
            );
        }

        OrganisasjonDokument org = brevbestilling.getOrg();
        Persondata personMottaker = brevbestilling.getPersonMottaker();
        return new Mottaker(
            mapNavn(org, personMottaker),
            mapAdresselinjer(org, brevbestilling.getKontaktpersonNavn(), brevbestilling.getKontaktopplysning(), personMottaker),
            mapPostnr(org, personMottaker),
            mapPoststed(org, personMottaker),
            mapLandForAdresse(org, personMottaker),
            mottakerType.getKode(),
            mapRegionForAdresse(org, personMottaker)
        );
    }
}
