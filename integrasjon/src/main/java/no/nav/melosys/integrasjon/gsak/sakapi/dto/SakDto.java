package no.nav.melosys.integrasjon.gsak.sakapi.dto;

public class SakDto {

    private Long id;
    private String tema; // https://kodeverkviewer.adeo.no/kodeverk/xml/fagomrade.xml
    private String applikasjon; // Fagsystemkode
    private String fagsakNr;
    private String aktoerId;
    private String orgnr;
    private String opprettetAv;
    // FIXME: Lagres som LocalDateTime i Sak API, men eksponeres som ZonedDateTime
    private String opprettetTidspunkt;

    public SakDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getApplikasjon() {
        return applikasjon;
    }

    public void setApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
    }

    public String getFagsakNr() {
        return fagsakNr;
    }

    public void setFagsakNr(String fagsakNr) {
        this.fagsakNr = fagsakNr;
    }

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

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public void setOpprettetAv(String opprettetAv) {
        this.opprettetAv = opprettetAv;
    }

    public String getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(String opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }
}
