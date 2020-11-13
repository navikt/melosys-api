package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public class SaksbehandlingstidSoknad extends Flettedata {
    private final LocalDateTime datoMottatt;
    private final LocalDateTime datoBehandlingstid;
    private final Sakstyper typeSoknad;
    private final Aktoersroller avsenderTypeSoknad;
    private final boolean mottakerRepresentantForBruker;
    private final String avsenderSoknad;
    private final String avsenderLand;

    public SaksbehandlingstidSoknad(String fodselsnr, String saksnummer, LocalDateTime dagensDato,
                                    LocalDateTime datoMottatt, LocalDateTime datoBehandlingstid,
                                    String navnBruker, String navnMottaker, List<String> adresselinjer,
                                    String postnr, String poststed, Sakstyper typeSoknad,
                                    Aktoersroller avsenderTypeSoknad, boolean mottakerRepresentantForBruker,
                                    String avsenderSoknad, String avsenderLand) {
        super(fodselsnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed);
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.typeSoknad = typeSoknad;
        this.avsenderTypeSoknad = avsenderTypeSoknad;
        this.mottakerRepresentantForBruker = mottakerRepresentantForBruker;
        this.avsenderSoknad = avsenderSoknad;
        this.avsenderLand = avsenderLand;
    }

    public static SaksbehandlingstidSoknad map(Behandling behandling) throws TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();
        LocalDateTime datoMottatt = LocalDateTime.from(fagsak.getRegistrertDato());

        return new SaksbehandlingstidSoknad(personDokument.fnr, fagsak.getSaksnummer(), LocalDateTime.now(), datoMottatt,
            datoMottatt.plusDays(SAKSBEHANDLINGSTID_DAGER), personDokument.sammensattNavn, personDokument.sammensattNavn,
            personDokument.postadresse.adresselinjer(), personDokument.postadresse.postnr, personDokument.postadresse.poststed,
            fagsak.getType(), BRUKER, false, null, null);
    }

    public LocalDateTime getDatoMottatt() {
        return datoMottatt;
    }

    public LocalDateTime getDatoBehandlingstid() {
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
