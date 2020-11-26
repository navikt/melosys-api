package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
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

    private final boolean mottakerRepresentantForBruker;

    private SaksbehandlingstidKlage(String fnr, String saksnummer, Instant dagensDato,
                                   Instant datoMottatt, Instant datoBehandlingstid,
                                   String navnBruker, String navnMottaker, List<String> adresselinjer,
                                   String postnr, String poststed, String land, Instant datoVedtak,
                                   boolean mottakerRepresentantForBruker) {
        super(fnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed, land);
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.datoVedtak = datoVedtak;
        this.mottakerRepresentantForBruker = mottakerRepresentantForBruker;
    }

    public static SaksbehandlingstidKlage av(Behandling behandling, Instant forsendelseMottatt) throws TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();

        String land = personDokument.postadresse.land != null ? personDokument.postadresse.land.toString() : null;
        return new SaksbehandlingstidKlage(personDokument.fnr, fagsak.getSaksnummer(), Instant.now(), forsendelseMottatt,
            forsendelseMottatt.plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS), personDokument.sammensattNavn, personDokument.sammensattNavn,
            personDokument.postadresse.adresselinjer(), personDokument.postadresse.postnr, personDokument.postadresse.poststed, land,
            null, false);
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
    
    public boolean isMottakerRepresentantForBruker() {
        return mottakerRepresentantForBruker;
    }

}
