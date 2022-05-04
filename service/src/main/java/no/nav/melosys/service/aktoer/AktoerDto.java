package no.nav.melosys.service.aktoer;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.service.persondata.PersondataFasade;

public class AktoerDto {

    private String aktoerID;
    private String institusjonsID;
    private String orgnr;
    private String rolleKode;
    private String utenlandskPersonID;
    private String representererKode;
    private Long databaseID;
    private String personIdent;

    public String getAktoerID() {
        return aktoerID;
    }

    public void setAktoerID(String aktoerID) {
        this.aktoerID = aktoerID;
    }

    public String getInstitusjonsID() {
        return institusjonsID;
    }

    public void setInstitusjonsID(String institusjonsID) {
        this.institusjonsID = institusjonsID;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getRolleKode() {
        return rolleKode;
    }

    public void setRolleKode(String rolleKode) {
        this.rolleKode = rolleKode;
    }

    public String getUtenlandskPersonID() {
        return utenlandskPersonID;
    }

    public void setUtenlandskPersonID(String utenlandskPersonID) {
        this.utenlandskPersonID = utenlandskPersonID;
    }

    public String getRepresentererKode() {
        return representererKode;
    }

    public void setRepresentererKode(String representererKode) {
        this.representererKode = representererKode;
    }

    public Long getDatabaseID() {
        return databaseID;
    }

    public void setDatabaseID(Long databaseID) {
        this.databaseID = databaseID;
    }

    public String getPersonIdent() {
        return personIdent;
    }

    public void setPersonIdent(String personIdent) {
        this.personIdent = personIdent;
    }

    public static AktoerDto tilDto(Aktoer aktoer, PersondataFasade persondataFasade) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID(aktoer.getAktørId());
        aktoerDto.setInstitusjonsID(aktoer.getInstitusjonId());
        aktoerDto.setOrgnr(aktoer.getOrgnr());
        aktoerDto.setRolleKode(aktoer.getRolle().getKode());
        aktoerDto.setUtenlandskPersonID(aktoer.getUtenlandskPersonId());
        if (aktoer.getRepresenterer() != null) {
            aktoerDto.setRepresentererKode(aktoer.getRepresenterer().getKode());
        }
        aktoerDto.setDatabaseID(aktoer.getId());
        if (aktoer.getAktørId() != null) {
            aktoerDto.setPersonIdent(persondataFasade.hentFolkeregisterident(aktoer.getAktørId()));
        }
        return aktoerDto;
    }

}
