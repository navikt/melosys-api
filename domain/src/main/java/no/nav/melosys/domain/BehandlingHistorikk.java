package no.nav.melosys.domain;

import java.time.LocalDateTime;
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
@Table(name = "behandling_historikk")
public class BehandlingHistorikk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "dato", nullable = false, updatable = false)
    private LocalDateTime dato;

    @Column(name = "status", nullable = false, updatable = false)
    @Convert(converter = Behandlingsstatus.DbKonverterer.class)
    private Behandlingsstatus status;

    @Column(name = "ident", nullable = false, updatable = false)
    private String ident;
    
    @Column(name = "kommentar", nullable = false, updatable = false)
    private String kommentar;

    public long getId() {
        return id;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public LocalDateTime getDato() {
        return dato;
    }

    public void setDato(LocalDateTime dato) {
        this.dato = dato;
    }

    public Behandlingsstatus getStatus() {
        return status;
    }

    public void setStatus(Behandlingsstatus status) {
        this.status = status;
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getKommentar() {
        return kommentar;
    }

    public void setKommentar(String kommentar) {
        this.kommentar = kommentar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BehandlingHistorikk)) { // Implisitt nullsjekk
            return false;
        }
        BehandlingHistorikk that = (BehandlingHistorikk) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.behandling, that.behandling)
            && Objects.equals(this.dato, that.dato)
            && Objects.equals(this.kommentar, that.kommentar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandling, dato, kommentar);
    }

}
