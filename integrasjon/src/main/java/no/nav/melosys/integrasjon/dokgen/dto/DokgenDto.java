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
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;

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
    private final String navnMottaker;
    private final List<String> adresselinjer;
    private final String postnr;
    private String poststed;
    private final String land;

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;
    // Svarfrist mangelbrev 4 uker fra dato brevet blir generert.
    protected static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

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
}
