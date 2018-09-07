package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "behandling")
public class Behandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Column(name = "status", nullable = false, updatable = true)
    @Convert(converter = BehandlingStatus.DbKonverterer.class)
    private BehandlingStatus status;

    @Column(name = "beh_type", nullable = false, updatable = false)
    @Convert(converter = BehandlingType.DbKonverterer.class)
    private BehandlingType type;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "endret_dato", nullable = false, updatable = true)
    private LocalDateTime endretDato;

    @Column(name = "siste_opplysninger_hentet_dato", updatable = true)
    private LocalDateTime sisteOpplysningerHentetDato;

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Saksopplysning> saksopplysninger = new HashSet<>(1);

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<BehandlingHistorikk> behandlingshistorikk = new HashSet<>(1);

    public long getId() {
        return id;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public void setFagsak(Fagsak fagsak) {
        this.fagsak = fagsak;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public BehandlingType getType() {
        return type;
    }

    public void setType(BehandlingType type) {
        this.type = type;
    }
    
    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

    public LocalDateTime getSistOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(LocalDateTime sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }

    public Set<Saksopplysning> getSaksopplysninger() {
        return saksopplysninger;
    }

    public void setSaksopplysninger(Set<Saksopplysning> saksopplysninger) {
        this.saksopplysninger = saksopplysninger;
    }

    public Set<BehandlingHistorikk> getBehandlingshistorikk() {
        return behandlingshistorikk;
    }

    public void setBehandlingshistorikk(Set<BehandlingHistorikk> behandlingshistorikk) {
        this.behandlingshistorikk = behandlingshistorikk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandling)) { // Implisitt nullsjekk
            return false;
        }
        Behandling that = (Behandling) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.registrertDato, that.registrertDato)
            && Objects.equals(this.fagsak, that.fagsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrertDato, fagsak);
    }

}
