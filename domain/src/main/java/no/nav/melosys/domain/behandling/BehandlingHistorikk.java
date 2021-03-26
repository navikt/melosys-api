package no.nav.melosys.domain.behandling;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;

@Entity
@Table(name = "behandling_historikk")
public class BehandlingHistorikk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "dato", nullable = false, updatable = false)
    private LocalDateTime dato;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private Behandlingsstatus status;

    @Column(name = "ident", nullable = false, updatable = false)
    private String ident;
    
    @Column(name = "kommentar", nullable = false, updatable = false)
    private String kommentar;

    public Long getId() {
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
            return this.id .equals(that.getId());
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
