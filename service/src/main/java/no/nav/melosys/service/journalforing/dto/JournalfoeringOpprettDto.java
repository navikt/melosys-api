package no.nav.melosys.service.journalforing.dto;

public class JournalfoeringOpprettDto extends JournalfoeringDto {
    private FagsakDto fagsak;
    private AnmodningOmUnntakDto anmodningOmUnntak;
    private String arbeidsgiverID;
    private String representantID;
    private String representantKontaktPerson;

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
}
