package no.nav.melosys.service.aktoer;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;

public class AktoerDto {

    private String aktoerID;
    private String personIdent;
    private String institusjonsID;
    private String orgnr;
    private String rolleKode;
    private String utenlandskPersonID;
    private Set<Fullmaktstype> fullmakter;
    private Long databaseID;

    public String getAktoerID() {
        return aktoerID;
    }

    public void setAktoerID(String aktoerID) {
        this.aktoerID = aktoerID;
    }

    public String getPersonIdent() {
        return personIdent;
    }

    public void setPersonIdent(String personIdent) {
        this.personIdent = personIdent;
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

    public Set<Fullmaktstype> getFullmakter() {
        return fullmakter;
    }

    public void setFullmakter(Set<Fullmaktstype> fullmakter) {
        this.fullmakter = fullmakter;
    }

    public Long getDatabaseID() {
        return databaseID;
    }

    public void setDatabaseID(Long databaseID) {
        this.databaseID = databaseID;
    }

    public static AktoerDto tilDto(Aktoer aktoer) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID(aktoer.getAktørId());
        aktoerDto.setInstitusjonsID(aktoer.getInstitusjonId());
        aktoerDto.setOrgnr(aktoer.getOrgnr());
        aktoerDto.setRolleKode(aktoer.getRolle().getKode());
        aktoerDto.setUtenlandskPersonID(aktoer.getUtenlandskPersonId());
        aktoerDto.setFullmakter(aktoer.getFullmaktstyper());
        aktoerDto.setDatabaseID(aktoer.getId());
        aktoerDto.setPersonIdent(aktoer.getPersonIdent());
        return aktoerDto;
    }
}
