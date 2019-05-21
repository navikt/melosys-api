package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.*;

@Entity
@Table(name = "behandlingsresultat_begrunnelse")
public class BehandlingsresultatBegrunnelse {

    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "kode")
    private String kode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BehandlingsresultatBegrunnelse)) {
            return false;
        }
        BehandlingsresultatBegrunnelse that = (BehandlingsresultatBegrunnelse) o;
        return Objects.equals(this.kode, that.kode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode);
    }
}
