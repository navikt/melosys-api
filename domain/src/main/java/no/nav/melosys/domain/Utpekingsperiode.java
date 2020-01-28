package no.nav.melosys.domain;

import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

@Entity
@Table(name = "utpekingsperiode")
public class Utpekingsperiode implements ErPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fom;

    @Column(name = "tom_dato", nullable = false, updatable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalgsland", updatable = false)
    private Landkoder lovvalgsland;

    @Column(name = "lovvalgsbestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse lovvalgsbestemmelse;

    @Column(name = "tilleggsbestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse tilleggsbestemmelse;

    @SuppressWarnings("unused") // Trengs av Hibernate
    public Utpekingsperiode() {
    }

    public Utpekingsperiode(LocalDate fom, LocalDate tom, Landkoder lovvalgsland,
                            LovvalgBestemmelse lovvalgsbestemmelse, LovvalgBestemmelse tilleggsbestemmelse) {
        this.fom = fom;
        this.tom = tom;
        this.lovvalgsland = lovvalgsland;
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
        this.tilleggsbestemmelse = tilleggsbestemmelse;
    }

    public Long getId() {
        return id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    @Override
    public LocalDate getFom() {
        return fom;
    }

    @Override
    public LocalDate getTom() {
        return tom;
    }

    public Landkoder getLovvalgsland() {
        return lovvalgsland;
    }

    public LovvalgBestemmelse getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public LovvalgBestemmelse getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }
}
