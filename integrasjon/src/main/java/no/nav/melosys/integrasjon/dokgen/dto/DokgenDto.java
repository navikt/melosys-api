package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@JsonInclude(Include.NON_EMPTY)
public abstract class DokgenDto {
    private final String fnr;
    private final String saksnummer;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant dagensDato;

    private final String navnBruker;
    private final String navnMottaker;
    private final List<String> adresselinjer;
    private final String postnr;
    private String poststed;
    private final String land;

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;

    protected DokgenDto(String fnr, String saksnummer, Instant dagensDato,
                        String navnBruker, String navnMottaker, List<String> adresselinjer,
                        String postnr, String poststed, String land) {
        this.fnr = fnr;
        this.saksnummer = saksnummer;
        this.dagensDato = dagensDato;
        this.navnBruker = navnBruker;
        this.navnMottaker = navnMottaker;
        this.adresselinjer = adresselinjer;
        this.postnr = postnr;
        this.poststed = poststed;
        this.land = land;
    }

    public String getFnr() {
        return fnr;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public Instant getDagensDato() {
        return dagensDato;
    }

    public String getNavnBruker() {
        return navnBruker;
    }

    public String getNavnMottaker() {
        return navnMottaker;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public String getPostnr() {
        return postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getLand() {
        return land;
    }

    protected static List<String> mapAdresselinjer(OrganisasjonDokument org, Kontaktopplysning kontaktopplysning, PersonDokument personDokument) {
        List<String> adresselinjer;
        if (org == null) {
            adresselinjer = personDokument.gjeldendePostadresse.adresselinjer();
        } else {
            if (kontaktopplysning != null) {
                adresselinjer = asList(
                    "v/" + kontaktopplysning.getKontaktNavn(),
                    org.getPostadresse().gatenavn +
                        ((org.getPostadresse().husnummer == null) ? "" : " " + org.getPostadresse().husnummer)
                );
            } else {
                adresselinjer = singletonList(org.getPostadresse().gatenavn +
                    ((org.getPostadresse().husnummer == null) ? "" : " " + org.getPostadresse().husnummer));
            }
        }
        return adresselinjer;
    }

    protected static String mapPostnr(OrganisasjonDokument org, PersonDokument personDokument) {
        return (org == null) ? personDokument.gjeldendePostadresse.postnr : org.getPostadresse().postnummer;
    }

    protected static String mapPostSted(OrganisasjonDokument org, PersonDokument personDokument) {
        return (org == null) ? personDokument.gjeldendePostadresse.poststed : org.getPostadresse().poststed;
    }

    protected static String mapLandForAdresse(OrganisasjonDokument organisasjonDokument, PersonDokument personDokument) {
        String land;
        if (organisasjonDokument == null) {
            land = personDokument.gjeldendePostadresse.land != null ? personDokument.gjeldendePostadresse.land.toString() : null;
        } else {
            land = organisasjonDokument.getPostadresse().landkode != null ? organisasjonDokument.getPostadresse().landkode : null;
        }
        return land;
    }
}
