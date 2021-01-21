package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;

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
    protected static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

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

    protected DokgenDto(DokgenBrevbestilling brevbestilling) throws TekniskException {
        Behandling behandling = brevbestilling.getBehandling();
        OrganisasjonDokument org = brevbestilling.getOrg();
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();

        this.fnr = personDokument.fnr;
        this.saksnummer = fagsak.getSaksnummer();
        this.dagensDato = Instant.now();
        this.navnBruker = personDokument.sammensattNavn;
        this.navnMottaker = (org == null ? personDokument.sammensattNavn : org.getNavn());
        this.adresselinjer = mapAdresselinjer(org, brevbestilling.getKontaktopplysning(), personDokument);
        this.postnr = mapPostnr(org, personDokument);
        this.poststed = mapPoststed(org, personDokument);
        this.land = mapLandForAdresse(org, personDokument);
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
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            if (kontaktopplysning != null) {
                adresselinjer = asList(
                    "v/" + kontaktopplysning.getKontaktNavn(),
                    orgAdresse.gatenavn +
                        ((orgAdresse.husnummer == null) ? "" : " " + orgAdresse.husnummer)
                );
            } else {
                adresselinjer = singletonList(orgAdresse.gatenavn +
                    ((orgAdresse.husnummer == null) ? "" : " " + orgAdresse.husnummer));
            }
        }
        return adresselinjer;
    }

    protected static String mapPostnr(OrganisasjonDokument org, PersonDokument personDokument) {
        String postNr;
        if (org == null) {
            postNr = personDokument.gjeldendePostadresse.postnr;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            postNr = orgAdresse.postnummer;
        }
        return postNr;
    }

    protected static String mapPoststed(OrganisasjonDokument org, PersonDokument personDokument) {
        String poststed;
        if (org == null) {
            poststed = personDokument.gjeldendePostadresse.poststed;
        } else {
            StrukturertAdresse orgAdresse = hentTilgjengeligAdresse(org);
            poststed = orgAdresse.poststed;
        }
        return poststed;
    }

    protected static String mapLandForAdresse(OrganisasjonDokument org, PersonDokument personDokument) {
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
