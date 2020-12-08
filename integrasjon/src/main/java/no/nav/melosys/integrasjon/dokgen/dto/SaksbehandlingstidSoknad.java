package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.TekniskException;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;

public class SaksbehandlingstidSoknad extends DokgenDto {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoBehandlingstid;

    private final Sakstyper typeSoknad;
    private final Aktoersroller avsenderTypeSoknad;
    private final String avsenderSoknad;
    private final String avsenderLand;

    private SaksbehandlingstidSoknad(String fnr, String saksnummer, Instant dagensDato,
                                     Instant datoMottatt, Instant datoBehandlingstid,
                                     String navnBruker, String navnMottaker, List<String> adresselinjer,
                                     String postnr, String poststed, String land, Sakstyper typeSoknad,
                                     Aktoersroller avsenderTypeSoknad, String avsenderSoknad, String avsenderLand) {
        super(fnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed, land);
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.typeSoknad = typeSoknad;
        this.avsenderTypeSoknad = avsenderTypeSoknad;
        this.avsenderSoknad = avsenderSoknad;
        this.avsenderLand = avsenderLand;
    }

    public static SaksbehandlingstidSoknad av(DokgenBrevbestilling brevbestilling) throws TekniskException {
        Behandling behandling = brevbestilling.getBehandling();
        OrganisasjonDokument org = brevbestilling.getOrg();
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();

        return new SaksbehandlingstidSoknad(
            personDokument.fnr,
            fagsak.getSaksnummer(),
            Instant.now(),
            brevbestilling.getForsendelseMottatt(),
            brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS),
            personDokument.sammensattNavn,
            (org == null ? personDokument.sammensattNavn : org.getNavn()),
            mapAdresselinjer(brevbestilling.getOrg(), brevbestilling.getKontaktopplysning(), personDokument),
            mapPostnr(brevbestilling.getOrg(), personDokument),
            mapPostSted(brevbestilling.getOrg(), personDokument),
            mapLandForAdresse(brevbestilling.getOrg(), personDokument),
            fagsak.getType(),
            (personDokument.fnr.equals(brevbestilling.getAvsenderId()) ? BRUKER : REPRESENTANT),
            brevbestilling.getAvsenderNavn(),
            null //NOTE Mangler inntil vi kan avgjøre om avsender == MYNDIGHET
        );
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public Sakstyper getTypeSoknad() {
        return typeSoknad;
    }

    public Aktoersroller getAvsenderTypeSoknad() {
        return avsenderTypeSoknad;
    }

    public String getAvsenderSoknad() {
        return avsenderSoknad;
    }

    public String getAvsenderLand() {
        return avsenderLand;
    }
}
