package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "behandling")
public class Behandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // FIXME (farjam 2017-10-06): Har vi en ekstern behandlingsId fra GSak??? I så fall bruk den for equals & hash.
    @Column(name = "gsak_id")
    private Long gsakId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fagsak_id", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Column(name = "status", nullable = false)
    @Convert(converter = BehandlingStatus.DbKonverterer.class)
    private BehandlingStatus status;

    @Column(name = "steg", nullable = false)
    @Convert(converter = BehandlingSteg.DbKonverterer.class)
    private BehandlingSteg steg;

    @Column(name = "type", nullable = false, updatable = false)
    @Convert(converter = BehandlingType.DbKonverterer.class)
    private BehandlingType type;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @OneToMany(mappedBy = "behandling", fetch = FetchType.EAGER)
    private Set<Saksopplysning> saksopplysninger;

    @OneToMany(mappedBy = "behandling", fetch = FetchType.EAGER)
    private Set<BehandlingHistorikk> behandlingshistorikk;

    public long getId() {
        return id;
    }

    public Long getGsakId() {
        return gsakId;
    }

    public void setGsakId(Long gsakId) {
        this.gsakId = gsakId;
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

    public BehandlingSteg getSteg() {
        return steg;
    }

    public void setSteg(BehandlingSteg steg) {
        this.steg = steg;
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
