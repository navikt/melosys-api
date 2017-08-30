package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
@Table(name = "BEHANDLING")
public class Behandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // FIXME (farjam): Har vi en ekstern behandlingsId fra GSak???
    // FIXME Francois Hvordan genererer vi? Eksponerer vi nøkkelen?
    //@Generated(GenerationTime.INSERT)
    @Column(name = "behandling_id")
    private Long behandlingId;

    @ManyToOne
    @JoinColumn(name = "fagsak_id", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Column(name = "status", nullable = false)
    @Convert(converter = BehandlingStatus.DbKonverterer.class)
    private BehandlingStatus status;

    // TODO (farjam 2017-08-18): Mangler kodeverk, se EESSI2-218
    @Column(name = "steg")
    private String steg;
    
    @ManyToOne
    @JoinColumn(name = "type", nullable = false, updatable = false)
    private BehandlingType type;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @OneToMany(mappedBy = "behandling", fetch = FetchType.EAGER)
    private List<Saksopplysning> saksopplysninger;

    @OneToMany(mappedBy = "behandling", fetch = FetchType.EAGER)
    private List<BehandlingHistorikk> behandlingshistorikk;

    public long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
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

    public String getSteg() {
        return steg;
    }

    public void setSteg(String steg) {
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

    public List<Saksopplysning> getSaksopplysninger() {
        return saksopplysninger;
    }

    public void setSaksopplysninger(List<Saksopplysning> saksopplysninger) {
        this.saksopplysninger = saksopplysninger;
    }

    public List<BehandlingHistorikk> getBehandlingshistorikk() {
        return behandlingshistorikk;
    }

    public void setBehandlingshistorikk(List<BehandlingHistorikk> behandlingshistorikk) {
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
        // TODO (farjam 2017-08-18): Her er det en teoretisk mulighet for feil oppførsel når vi sammenligner entiteter.
        // For å fikse dette må equals og hashCode legge en ikke-generert og unik nøkkel til grunn.
        // Dette er for det meste kun en teoretisk mulighet for feil. Det skal mye til for at to behandlinger blir registrert på samme sak i det samme nanosekundet.
        return Objects.equals(this.registrertDato, that.registrertDato)
            && Objects.equals(this.fagsak, that.fagsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrertDato, fagsak);
    }

}
