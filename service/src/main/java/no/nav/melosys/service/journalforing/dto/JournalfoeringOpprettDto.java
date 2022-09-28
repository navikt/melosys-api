package no.nav.melosys.service.journalforing.dto;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class JournalfoeringOpprettDto extends JournalfoeringDto {
    private FagsakDto fagsak;
    private String arbeidsgiverID;
    private String representantID;
    private String representantKontaktPerson;
    private String representererKode;

    public FagsakDto getFagsak() {
        return fagsak;
    }

    public void setFagsak(FagsakDto fagsak) {
        this.fagsak = fagsak;
    }

    public String getArbeidsgiverID() {
        return arbeidsgiverID;
    }

    public void setArbeidsgiverID(String arbeidsgiverID) {
        this.arbeidsgiverID = arbeidsgiverID;
    }

    public String getRepresentantID() {
        return representantID;
    }

    public void setRepresentantID(String representantID) {
        this.representantID = representantID;
    }

    public String getRepresentantKontaktPerson() {
        return representantKontaktPerson;
    }

    public void setRepresentantKontaktPerson(String representantKontaktPerson) {
        this.representantKontaktPerson = representantKontaktPerson;
    }

    public String getRepresentererKode() {
        return representererKode;
    }

    public void setRepresentererKode(String representererKode) {
        this.representererKode = representererKode;
    }

    public boolean erTomFlyt() {
        var sakstype = Sakstyper.valueOf(getFagsak().getSakstype());
        var behandlingstema = Behandlingstema.valueOf(getBehandlingstemaKode());
        var behandlingstype = Behandlingstyper.valueOf(getBehandlingstypeKode());

        if (List.of(Behandlingstyper.HENVENDELSE, Behandlingstyper.KLAGE).contains(behandlingstype)) {
            return true;
        }

        return switch (sakstype) {
            case FTRL -> behandlingstema == Behandlingstema.YRKESAKTIV;
            case TRYGDEAVTALE -> behandlingstema == Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL;
            case EU_EOS -> List.of(Behandlingstema.ARBEID_KUN_NORGE,
                Behandlingstema.PENSJONIST,
                Behandlingstema.REGISTRERING_UNNTAK,
                Behandlingstema.UNNTAK_MEDLEMSKAP,
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET).contains(behandlingstema);
        };
    }

    public boolean erBehandlingAvSøknad() {
        return Behandling.erBehandlingAvSøknad(getBehandlingstemaKode());
    }
}
