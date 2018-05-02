package no.nav.melosys.integrasjon.gsak.sakapi.dto;

public class SakSearchRequest {

    // Filtrering på saker opprettet for en aktør (person)
    private String aktoerId;
    // Filtrering på saker opprettet for en organisasjon
    private String orgnr;
    // Filtrering på applikasjon (iht felles kodeverk)
    private String applikasjon;
    // Filtrering på tema (iht felles kodeverk)
    private String tema;
    // Filtrering på fagsakNr")
    private String fagsakNr;
    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getApplikasjon() {
        return applikasjon;
    }

    public void setApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getFagsakNr() {
        return fagsakNr;
    }

    public void setFagsakNr(String fagsakNr) {
        this.fagsakNr = fagsakNr;
    }
}
