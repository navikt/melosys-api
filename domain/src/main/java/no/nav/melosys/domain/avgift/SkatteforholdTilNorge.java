package no.nav.melosys.domain.avgift;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.*;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;

@Entity
@Table(name = "skatteforhold_til_norge")
public class SkatteforholdTilNorge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @OneToMany(mappedBy = "grunnlagSkatteforholdTilNorge")
//    private Set<Trygdeavgiftsperiode> trygdeavgiftsperioder;

    @Column(name = "fom_dato", nullable = false)
    private LocalDate fomDato;

    @Column(name = "tom_dato", nullable = true)
    private LocalDate tomDato;

    @Column(name = "skatteplikt_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Skatteplikttype skatteplikttype;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public Skatteplikttype getSkatteplikttype() {
        return skatteplikttype;
    }

    public void setSkatteplikttype(Skatteplikttype skatteplikttype) {
        this.skatteplikttype = skatteplikttype;
    }
}
