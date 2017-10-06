package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "vedtak")
public class Vedtak {

    // Populeres av Hibernate med behandling.id
    @Id
    private long id; 

    @MapsId
    @OneToOne(fetch=FetchType.EAGER, optional = false)
    @JoinColumn(name="behandling_id")
    private Behandling behandling;
    
    @Column(name = "vedtak_dato", nullable = false, updatable = false)
    private LocalDateTime vedtakstDato;

    @OneToMany(mappedBy = "vedtak", fetch = FetchType.EAGER)
    private Set<LovvalgPeriode> LovvalgPerioder;

    public long getId() {
        return id;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public LocalDateTime getVedtakstDato() {
        return vedtakstDato;
    }

    public void setVedtakstDato(LocalDateTime vedtakstDato) {
        this.vedtakstDato = vedtakstDato;
    }

    public Set<LovvalgPeriode> getLovvalgPerioder() {
        return LovvalgPerioder;
    }

    public void setLovvalgPerioder(Set<LovvalgPeriode> lovvalgPerioder) {
        LovvalgPerioder = lovvalgPerioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vedtak)) {
            return false;
        }
        Vedtak that = (Vedtak) o;
        return Objects.equals(this.behandling, that.behandling);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(behandling);
    }
    
}
