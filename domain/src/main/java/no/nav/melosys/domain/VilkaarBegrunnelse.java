package no.nav.melosys.domain;

import java.util.Objects;
import jakarta.persistence.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "vilkaar_begrunnelse")
@EntityListeners(AuditingEntityListener.class)
public class VilkaarBegrunnelse extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vilkaar_resultat_id", nullable = false, updatable = false)
    private Vilkaarsresultat vilkaarsresultat;

    @Column(name = "kode")
    private String kode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vilkaarsresultat getVilkaarsresultat() {
        return vilkaarsresultat;
    }

    public void setVilkaarsresultat(Vilkaarsresultat vilkaarsresultat) {
        this.vilkaarsresultat = vilkaarsresultat;
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
        if (!(o instanceof VilkaarBegrunnelse)) {
            return false;
        }
        VilkaarBegrunnelse that = (VilkaarBegrunnelse) o;
        return Objects.equals(getVilkaarsresultat(), that.getVilkaarsresultat())
            && Objects.equals(getKode(), that.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVilkaarsresultat(), getKode());
    }
}
