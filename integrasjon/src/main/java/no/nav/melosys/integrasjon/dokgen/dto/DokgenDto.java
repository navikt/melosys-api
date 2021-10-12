package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.*;

@JsonInclude(Include.NON_EMPTY)
public abstract class DokgenDto {
    private final String fnr;
    private final String saksnummer;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant dagensDato;

    private final String navnBruker;
    private final String navnMottaker; // Erstattes av mottaker.navn
    private final List<String> adresselinjer; // Erstattes av mottaker.adresselinjer
    private final String postnr; // Erstattes av mottaker.postnr
    private String poststed; // Erstattes av mottaker.poststed
    private String land; // Erstattes av mottaker.land
    private final Mottaker mottaker;

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;
    // Svarfrist mangelbrev 4 uker fra dato brevet blir generert.
    protected static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    protected DokgenDto(DokgenBrevbestilling brevbestilling) {
        Behandling behandling = brevbestilling.getBehandling();
        OrganisasjonDokument org = brevbestilling.getOrg();
        Fagsak fagsak = behandling.getFagsak();
        Persondata persondata = brevbestilling.getPersondokument();

        this.fnr = persondata.hentFolkeregisterident();
        this.saksnummer = fagsak.getSaksnummer();
        this.dagensDato = Instant.now();
        this.navnBruker = persondata.getSammensattNavn();
        this.navnMottaker = mapMottakerNavn(org, persondata);
        this.adresselinjer = mapAdresselinjer(org, brevbestilling.getKontaktpersonNavn(), brevbestilling.getKontaktopplysning(), persondata);
        this.postnr = mapPostnr(org, persondata);
        this.poststed = mapPoststed(org, persondata);
        this.land = mapLandForAdresse(org, persondata);
        this.mottaker = mapMottaker(org, brevbestilling.getKontaktpersonNavn(), brevbestilling.getKontaktopplysning(), persondata);
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

    public void setLand(String land) {
        this.land = land;
    }

    public Mottaker getMottaker() {
        return mottaker;
    }
}
