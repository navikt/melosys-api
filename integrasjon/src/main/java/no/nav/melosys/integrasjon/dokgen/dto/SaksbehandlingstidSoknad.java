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
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.TekniskException;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public class SaksbehandlingstidSoknad extends DokgenDto {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoBehandlingstid;

    private final Sakstyper typeSoknad;
    private final Aktoersroller avsenderTypeSoknad;
    private final boolean mottakerRepresentantForBruker;
    private final String avsenderSoknad;
    private final String avsenderLand;

    private SaksbehandlingstidSoknad(String fnr, String saksnummer, Instant dagensDato,
                                    Instant datoMottatt, Instant datoBehandlingstid,
                                    String navnBruker, String navnMottaker, List<String> adresselinjer,
                                    String postnr, String poststed, Sakstyper typeSoknad,
                                    Aktoersroller avsenderTypeSoknad, boolean mottakerRepresentantForBruker,
                                    String avsenderSoknad, String avsenderLand) {
        super(fnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed);
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.typeSoknad = typeSoknad;
        this.avsenderTypeSoknad = avsenderTypeSoknad;
        this.mottakerRepresentantForBruker = mottakerRepresentantForBruker;
        this.avsenderSoknad = avsenderSoknad;
        this.avsenderLand = avsenderLand;
    }

    public static SaksbehandlingstidSoknad av(Behandling behandling, Instant forsendelseMottatt) throws TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();

        return new SaksbehandlingstidSoknad(personDokument.fnr, fagsak.getSaksnummer(), Instant.now(), forsendelseMottatt,
            forsendelseMottatt.plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS), personDokument.sammensattNavn, personDokument.sammensattNavn,
            personDokument.postadresse.adresselinjer(), personDokument.postadresse.postnr, personDokument.postadresse.poststed,
            fagsak.getType(), BRUKER, false, null, null);
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

    public boolean isMottakerRepresentantForBruker() {
        return mottakerRepresentantForBruker;
    }

    public String getAvsenderSoknad() {
        return avsenderSoknad;
    }

    public String getAvsenderLand() {
        return avsenderLand;
    }
}
