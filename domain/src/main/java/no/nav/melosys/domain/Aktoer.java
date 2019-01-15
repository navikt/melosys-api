package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.Aktoerroller;

@Entity
@Table(name = "aktoer")
public class Aktoer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name="saksnummer", updatable = false)
    private Fagsak fagsak;
    
    @Column(name = "aktoer_id", updatable = false)
    private String aktørId;

    @Column(name = "orgnr")
    private String orgnr;
    
    @Column(name = "rolle", nullable = false, updatable = false)
    @Convert(converter = Aktoerroller.DbKonverterer.class)
    private Aktoerroller rolle;

    @Column(name = "utenlandsk_id")
    private String utenlandskId;

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

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public Aktoerroller getRolle() {
        return rolle;
    }

    public void setRolle(Aktoerroller rolle) {
        this.rolle = rolle;
    }

    public String getUtenlandskId() {
        return utenlandskId;
    }

    public void setUtenlandskId(String utenlandskId) {
        this.utenlandskId = utenlandskId;
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
        return Objects.equals(this.fagsak, that.fagsak)
            && Objects.equals(this.aktørId, that.aktørId)
            && Objects.equals(this.orgnr, that.orgnr)
            && Objects.equals(this.utenlandskId, that.utenlandskId)
            && Objects.equals(this.rolle, that.rolle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsak, aktørId, orgnr, utenlandskId, rolle);
    }
    
}
