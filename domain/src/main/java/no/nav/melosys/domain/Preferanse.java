package no.nav.melosys.domain;

import javax.persistence.*;

@Entity
@Table(name = "preferanse")
public class Preferanse {

    public enum PreferanseEnum {
        RESERVERT_FRA_A1
    }

    // For Hibernate
    private Preferanse() {
    }

    public Preferanse(long id, PreferanseEnum preferanse) {
        this.id = id;
        this.preferanse = preferanse;
    }

    @Id
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kode")
    private PreferanseEnum preferanse;

    public PreferanseEnum getPreferanse() {
        return preferanse;
    }
}