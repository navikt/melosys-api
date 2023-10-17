package no.nav.melosys.service.journalforing.dto;


import no.nav.melosys.domain.kodeverk.Fullmaktstype;

import java.util.List;

public class JournalfoeringOpprettDto extends JournalfoeringDto {
    private FagsakDto fagsak;
    @Deprecated(since = "Fjernes snarest med tilfølgende kode. Sendes aldri fra frontend")
    private String arbeidsgiverID;
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    private String representantID;
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    private String representantKontaktPerson;
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    private String representererKode;
    private String fullmektigID;
    private List<Fullmaktstype> fullmakter;
    private String fullmektigKontaktperson;
    private String fullmektigKontaktOrgnr;

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

    public String getFullmektigID() {
        return fullmektigID;
    }

    public void setFullmektigID(String fullmektigID) {
        this.fullmektigID = fullmektigID;
    }

    public List<Fullmaktstype> getFullmakter() {
        return fullmakter;
    }

    public void setFullmakter(List<Fullmaktstype> fullmakter) {
        this.fullmakter = fullmakter;
    }

    public String getFullmektigKontaktperson() {
        return fullmektigKontaktperson;
    }

    public void setFullmektigKontaktperson(String fullmektigKontaktperson) {
        this.fullmektigKontaktperson = fullmektigKontaktperson;
    }

    public String getFullmektigKontaktOrgnr() {
        return fullmektigKontaktOrgnr;
    }

    public void setFullmektigKontaktOrgnr(String fullmektigKontaktOrgnr) {
        this.fullmektigKontaktOrgnr = fullmektigKontaktOrgnr;
    }
}
