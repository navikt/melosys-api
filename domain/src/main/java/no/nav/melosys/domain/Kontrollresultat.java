package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.behandling.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;

@Entity
@Table(name = "kontrollresultat")
public class Kontrollresultat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "begrunnelse", nullable = false, updatable = false)
    private Kontroll_begrunnelser begrunnelse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Kontroll_begrunnelser getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(Kontroll_begrunnelser begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kontrollresultat that = (Kontrollresultat) o;
        return id.equals(that.id) &&
            begrunnelse == that.begrunnelse &&
            behandlingsresultat.equals(that.behandlingsresultat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, begrunnelse, behandlingsresultat);
    }
}
