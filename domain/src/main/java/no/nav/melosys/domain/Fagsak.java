package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "fagsak")
public class Fagsak {
    
    // FIXME (farjam): Ikke tatt med fra logisk modell: tema, virkemiddel og registreringsmetainfo

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /** Saksnummer fra gsak */
    @Column(name = "saksnummer")
    private Long saksnummer;
    
    @Column(name = "fagsak_type", nullable = false, updatable = false)
    @Convert(converter = FagsakType.DbKonverterer.class)
    private FagsakType type;

    @Column(name = "versjon", nullable = false, updatable = false)
    private int versjon;
    
    @Column(name = "status", nullable = false)
    @Convert(converter = FagsakStatus.DbKonverterer.class)
    private FagsakStatus status;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @OneToMany(mappedBy = "fagsak", fetch = FetchType.EAGER)
    private Set<Aktoer> aktører;

    @OneToMany(mappedBy = "fagsak")
    private List<Behandling> behandlinger;

    public long getId() {
        return id;
    }

    public Long getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Long saksnummer) {
        this.saksnummer = saksnummer;
    }

    public FagsakType getType() {
        return type;
    }

    public void setType(FagsakType type) {
        this.type = type;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Set<Aktoer> getAktører() {
        return aktører;
    }

    public void setAktører(Set<Aktoer> aktører) {
        this.aktører = aktører;
    }

    public List<Behandling> getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(List<Behandling> behandlinger) {
        this.behandlinger = behandlinger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fagsak)) { // Implisitt nullsjekk
            return false;
        }
        Fagsak that = (Fagsak) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.saksnummer, that.saksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }

}
