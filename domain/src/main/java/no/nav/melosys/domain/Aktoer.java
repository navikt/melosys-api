package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "AKTOER")
public abstract class Aktoer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="fagsak_id", updatable = false)
    private Fagsak fagsak;
    
    @Column(name = "aktoer_id", updatable = false)
    private String aktørId;

    @Column(name = "ekstern_id")
    private String eksternId;
    
    @Column(name = "rolle", nullable = false, updatable = false)
    @Convert(converter = RolleType.DbKonverterer.class)
    private RolleType rolle;

    public Long getId() {
        return id;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public String getEksternId() {
        return eksternId;
    }

    public void setEksternId(String eksternId) {
        this.eksternId = eksternId;
    }

    public RolleType getRolle() {
        return rolle;
    }

    public void setRolle(RolleType rolle) {
        this.rolle = rolle;
    }

}
