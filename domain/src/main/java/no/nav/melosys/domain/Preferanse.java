package no.nav.melosys.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "preferanse")
public class Preferanse {

    public enum PreferanseEnum {
        RESERVERT_FRA_A1
    }

    // For Hibernate
    private Preferanse() {
    }

    public Preferanse(Long id, PreferanseEnum preferanse) {
        this.id = id;
        this.preferanse = preferanse;
    }

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kode")
    private PreferanseEnum preferanse;

    public PreferanseEnum getPreferanse() {
        return preferanse;
    }
}
