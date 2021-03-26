package no.nav.melosys.service.journalforing.dto;

import no.nav.melosys.domain.behandling.Behandling;

public class JournalfoeringOpprettDto extends JournalfoeringDto {
    private String behandlingstemaKode;
    private FagsakDto fagsak;
    @Deprecated(forRemoval = true) //TODO: må fjernes fra schema
    private AnmodningOmUnntakDto anmodningOmUnntak;
    private String arbeidsgiverID;
    private String representantID;
    private String representantKontaktPerson;
    private String representererKode;

    public String getBehandlingstemaKode() {
        return behandlingstemaKode;
    }

    public void setBehandlingstemaKode(String behandlingstemaKode) {
        this.behandlingstemaKode = behandlingstemaKode;
    }

    public FagsakDto getFagsak() {
        return fagsak;
    }

    public void setFagsak(FagsakDto fagsak) {
        this.fagsak = fagsak;
    }

    public AnmodningOmUnntakDto getAnmodningOmUnntak() {
        return anmodningOmUnntak;
    }

    public void setAnmodningOmUnntak(AnmodningOmUnntakDto anmodningOmUnntak) {
        this.anmodningOmUnntak = anmodningOmUnntak;
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

    public boolean erBehandlingAvSøknad() {
        return Behandling.erBehandlingAvSøknad(getBehandlingstemaKode());
    }
}
