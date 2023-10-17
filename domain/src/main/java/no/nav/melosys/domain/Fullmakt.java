package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Fullmaktstype;

import javax.persistence.*;

@Entity
@Table(name = "fullmakt")
public class Fullmakt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aktoer_id", updatable = false)
    private Aktoer aktoer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Fullmaktstype type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aktoer getAktoer() {
        return aktoer;
    }

    public void setAktoer(Aktoer aktoer) {
        this.aktoer = aktoer;
    }

    public Fullmaktstype getType() {
        return type;
    }

    public void setType(Fullmaktstype type) {
        this.type = type;
    }
}
