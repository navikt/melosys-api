package no.nav.melosys.domain;

import java.time.Instant;
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
    @Convert(converter = Behandlingsstatus.DbKonverterer.class)
    private Behandlingsstatus status;

    @Column(name = "beh_type", nullable = false, updatable = false)
    @Convert(converter = Behandlingstype.DbKonverterer.class)
    private Behandlingstype type;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false, updatable = true)
    private Instant endretDato;

    @Column(name = "siste_opplysninger_hentet_dato", updatable = true)
    private Instant sisteOpplysningerHentetDato;

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

    public Behandlingsstatus getStatus() {
        return status;
    }

    public void setStatus(Behandlingsstatus status) {
        this.status = status;
    }

    public Behandlingstype getType() {
        return type;
    }

    public void setType(Behandlingstype type) {
        this.type = type;
    }
    
    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public Instant getSistOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
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
