package no.nav.melosys.service.aktoer;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Representerer;

public class AktoerDto {

    private String aktoerID;
    private String personIdent;
    private String institusjonsID;
    private String orgnr;
    private String rolleKode;
    private String utenlandskPersonID;
    private String representererKode;
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

    public String getRepresentererKode() {
        return representererKode;
    }

    public void setRepresentererKode(String representererKode) {
        this.representererKode = representererKode;
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
        if (aktoer.getRepresenterer() != null) {
            aktoerDto.setRepresentererKode(aktoer.getRepresenterer().getKode());
        }
        aktoerDto.setFullmakter(aktoer.getFullmaktstyper());
        aktoerDto.setDatabaseID(aktoer.getId());
        aktoerDto.setPersonIdent(aktoer.getPersonIdent());
        midlertidigStøtteForBådeRepresentantOgFullmektig(aktoer, aktoerDto);
        return aktoerDto;
    }

    @Deprecated(since = "trengs ikke etter MELOSYS_FULLMAKT_TRYGDEAVGIFT fjernes og kodene er migrert")
    private static void midlertidigStøtteForBådeRepresentantOgFullmektig(Aktoer aktoer, AktoerDto aktoerDto) {
        if (aktoer.getRolle() == Aktoersroller.REPRESENTANT && aktoer.getRepresenterer() != null) {
            switch (aktoer.getRepresenterer()) {
                case BRUKER -> aktoer.setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD);
                case ARBEIDSGIVER -> aktoer.setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
                case BEGGE -> aktoer.setFullmaktstyper(Set.of(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));
            }
        }
        if (aktoer.getFullmaktstyper().contains(Fullmaktstype.FULLMEKTIG_SØKNAD)) {
            var erFullmektigSøknad = aktoer.getFullmaktstyper().contains(Fullmaktstype.FULLMEKTIG_SØKNAD);
            var erFullmektigArbeidsgiver = aktoer.getFullmaktstyper().contains(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);

            if (erFullmektigSøknad && erFullmektigArbeidsgiver) {
                aktoerDto.setRepresentererKode(Representerer.BEGGE.getKode());
            } else if (erFullmektigSøknad) {
                aktoerDto.setRepresentererKode(Representerer.BRUKER.getKode());
            } else if (erFullmektigArbeidsgiver) {
                aktoerDto.setRepresentererKode(Representerer.ARBEIDSGIVER.getKode());
            }
        }
    }

}
