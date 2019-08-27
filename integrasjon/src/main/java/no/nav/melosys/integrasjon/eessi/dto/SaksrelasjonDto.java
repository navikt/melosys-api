package no.nav.melosys.integrasjon.eessi.dto;

public class SaksrelasjonDto {
    private Long gsakSaksnummer;
    private String rinaSaksnummer;
    private String bucType;

    public SaksrelasjonDto() {
    }

    public SaksrelasjonDto(Long gsakSaksnummer, String rinaSaksnummer, String bucType) {
        this.gsakSaksnummer = gsakSaksnummer;
        this.rinaSaksnummer = rinaSaksnummer;
        this.bucType = bucType;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public void setRinaSaksnummer(String rinaSaksnummer) {
        this.rinaSaksnummer = rinaSaksnummer;
    }

    public String getBucType() {
        return bucType;
    }

    public void setBucType(String bucType) {
        this.bucType = bucType;
    }
}