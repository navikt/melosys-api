package no.nav.melosys.integrasjon.sak.dto;

public class SakSearchRequest {

    private String aktørId; // Filtrering på saker opprettet for en aktør (person)
    private String orgnr; // Filtrering på saker opprettet for en organisasjon
    private String applikasjon; // Filtrering på applikasjon (iht felles kodeverk)
    private String tema; // Filtrering på tema (iht felles kodeverk)
    private String fagsakNr; // Filtrering på fagsakNr")

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
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
