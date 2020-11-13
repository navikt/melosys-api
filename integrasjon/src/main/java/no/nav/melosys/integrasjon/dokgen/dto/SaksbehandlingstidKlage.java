package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;

public class SaksbehandlingstidKlage extends Flettedata {
    private final LocalDateTime datoMottatt;
    private final LocalDateTime datoBehandlingstid;
    private final LocalDateTime datoVedtak;
    private final boolean mottakerRepresentantForBruker;

    public SaksbehandlingstidKlage(String fodselsnr, String saksnummer, LocalDateTime dagensDato,
                                   LocalDateTime datoMottatt, LocalDateTime datoBehandlingstid,
                                   String navnBruker, String navnMottaker, List<String> adresselinjer,
                                   String postnr, String poststed, LocalDateTime datoVedtak,
                                   boolean mottakerRepresentantForBruker) {
        super(fodselsnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed);
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.datoVedtak = datoVedtak;
        this.mottakerRepresentantForBruker = mottakerRepresentantForBruker;
    }

    public static SaksbehandlingstidKlage map(Behandling behandling) throws TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();
        LocalDateTime datoMottatt = LocalDateTime.from(fagsak.getRegistrertDato());

        return new SaksbehandlingstidKlage(personDokument.fnr, fagsak.getSaksnummer(), LocalDateTime.now(), datoMottatt,
            datoMottatt.plusDays(SAKSBEHANDLINGSTID_DAGER), personDokument.sammensattNavn, personDokument.sammensattNavn,
            personDokument.postadresse.adresselinjer(), personDokument.postadresse.postnr, personDokument.postadresse.poststed,
            null, false);
    }

    public LocalDateTime getDatoMottatt() {
        return datoMottatt;
    }

    public LocalDateTime getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public LocalDateTime getDatoVedtak() {
        return datoVedtak;
    }
    public boolean isMottakerRepresentantForBruker() {
        return mottakerRepresentantForBruker;
    }

}
