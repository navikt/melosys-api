package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapMottaker;

@JsonInclude(Include.NON_EMPTY)
public abstract class DokgenDto {
    private final String fnr;
    private final String saksnummer;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant dagensDato;

    private final String navnBruker;
    private Mottaker mottaker;

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;
    // Svarfrist mangelbrev 4 uker fra dato brevet blir generert.
    protected static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    protected DokgenDto(DokgenBrevbestilling brevbestilling) {
        Persondata persondata = brevbestilling.getPersondokument();

        this.fnr = persondata.hentFolkeregisterident();
        this.saksnummer = brevbestilling.getBehandling().getFagsak().getSaksnummer();
        this.dagensDato = Instant.now();
        this.navnBruker = persondata.getSammensattNavn();
        this.mottaker = mapMottaker(brevbestilling.getOrg(), brevbestilling.getKontaktpersonNavn(),
            brevbestilling.getKontaktopplysning(), persondata, Aktoersroller.BRUKER);
    }

    protected DokgenDto(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        Persondata persondata = brevbestilling.getPersondokument();

        this.fnr = persondata.hentFolkeregisterident();
        this.saksnummer = brevbestilling.getBehandling().getFagsak().getSaksnummer();
        this.dagensDato = Instant.now();
        this.navnBruker = persondata.getSammensattNavn();
        this.mottaker = mapMottaker(brevbestilling.getOrg(), brevbestilling.getKontaktpersonNavn(),
            brevbestilling.getKontaktopplysning(), persondata, mottakerType);
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

    public Mottaker getMottaker() {
        return mottaker;
    }

    public void setMottaker(Mottaker mottaker) {
        this.mottaker = mottaker;
    }
}
