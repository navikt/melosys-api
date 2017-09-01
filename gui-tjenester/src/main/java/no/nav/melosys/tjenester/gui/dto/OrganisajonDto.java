package no.nav.melosys.tjenester.gui.dto;

public class OrganisajonDto {

    private String orgnummer;

    private String navn;
    
    private OrganisasjonsDetaljerDto organisasjonDetaljer;

    public OrganisajonDto() {
    }

    /**
     *
     * @param orgnummer
     */
    public OrganisajonDto(String orgnummer) {
        super();
        this.orgnummer = orgnummer;
    }

    public String getOrgnummer() {
        return orgnummer;
    }

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public OrganisasjonsDetaljerDto getOrganisasjonDetaljer() {
        return organisasjonDetaljer;
    }

    public void setOrganisasjonDetaljer(OrganisasjonsDetaljerDto organisasjonDetaljer) {
        this.organisasjonDetaljer = organisasjonDetaljer;
    }

}
