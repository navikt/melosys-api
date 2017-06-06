package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "BEHANDLING_RESULTAT")
public class Behandlingsresultat {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGen")
    @SequenceGenerator(name= "idGen", sequenceName = "SEQ_BEHANDLING_RESULTAT")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "behandling_maate")
    private BehandlingsMaate behandlingsMåte;

    @OneToOne(mappedBy = "resultat")
    private Behandling behandling;

    @OneToOne
    @JoinColumn(name = "rettighet_id")
    private FastsattRettighet rettighet;

    @OneToOne
    @JoinColumn(name = "vedtak_id")
    private Vedtak vedtak;

    public Long getId() {
        return id;
    }

    public BehandlingsMaate getBehandlingsMåte() {
        return behandlingsMåte;
    }

    public void setBehandlingsMåte(BehandlingsMaate behandlingsMåte) {
        this.behandlingsMåte = behandlingsMåte;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public FastsattRettighet getRettighet() {
        return rettighet;
    }

    public void setRettighet(FastsattRettighet rettighet) {
        this.rettighet = rettighet;
    }

    public Vedtak getVedtak() {
        return vedtak;
    }

    public void setVedtak(Vedtak vedtak) {
        this.vedtak = vedtak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Behandlingsresultat b = (Behandlingsresultat) o;
        return Objects.equals(behandlingsMåte, b.getBehandlingsMåte())
                && Objects.equals(behandling, b.getBehandling())
                && Objects.equals(rettighet, b.getRettighet())
                && Objects.equals(vedtak, b.getVedtak());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsMåte, behandling, rettighet, vedtak);
    }

}
