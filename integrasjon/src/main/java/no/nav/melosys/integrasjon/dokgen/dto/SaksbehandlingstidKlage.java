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
import no.nav.melosys.exception.TekniskException;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class SaksbehandlingstidKlage extends DokgenDto {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoBehandlingstid;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoVedtak;

    private SaksbehandlingstidKlage(String fnr, String saksnummer, Instant dagensDato,
                                    Instant datoMottatt, Instant datoBehandlingstid,
                                    String navnBruker, String navnMottaker, List<String> adresselinjer,
                                    String postnr, String poststed, String land, Instant datoVedtak) {
        super(fnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed, land);
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.datoVedtak = datoVedtak;
    }

    public static SaksbehandlingstidKlage av(DokgenBrevbestilling brevbestilling) throws TekniskException {
        Behandling behandling = brevbestilling.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        OrganisasjonDokument org = brevbestilling.getOrg();
        PersonDokument personDokument = behandling.hentPersonDokument();

        return new SaksbehandlingstidKlage(
            personDokument.fnr,
            fagsak.getSaksnummer(),
            Instant.now(),
            brevbestilling.getForsendelseMottatt(),
            brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS),
            personDokument.sammensattNavn,
            (org == null ? personDokument.sammensattNavn : org.getNavn()),
            mapAdresselinjer(org, brevbestilling.getKontaktopplysning(), personDokument),
            mapPostnr(org, personDokument),
            mapPoststed(org, personDokument),
            mapLandForAdresse(org, personDokument),
            null
        );
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public Instant getDatoVedtak() {
        return datoVedtak;
    }

}
