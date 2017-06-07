package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Aktoer {

    @Column(name = "aktoer_id")
    private Long aktørId;

    @Column(name = "org_nummer")
    private Long orgNummer;

    public Long getAktørId() {
        return aktørId;
    }

    public void setAktørId(Long aktørId) {
        this.aktørId = aktørId;
    }

    public Long getOrgNummer() {
        return orgNummer;
    }

    public void setOrgNummer(Long orgNummer) {
        this.orgNummer = orgNummer;
    }
}
