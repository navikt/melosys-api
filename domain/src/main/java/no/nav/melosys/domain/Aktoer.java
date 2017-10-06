package no.nav.melosys.domain;

import java.util.Objects;

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
@Table(name = "aktoer")
public abstract class Aktoer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name="fagsak_id", updatable = false)
    private Fagsak fagsak;
    
    @Column(name = "aktoer_id", updatable = false)
    private String aktørId;

    /** Kan være orgnr. eller personnr. */
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

    public void setFagsak(Fagsak fagsak) {
        this.fagsak = fagsak;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Aktoer)) {
            return false;
        }
        Aktoer that = (Aktoer) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.fagsak, that.fagsak)
            && Objects.equals(this.aktørId, that.aktørId)
            && Objects.equals(this.rolle, that.rolle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsak, aktørId, rolle);
    }
    
}
