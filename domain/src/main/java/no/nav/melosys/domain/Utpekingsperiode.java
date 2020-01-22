package no.nav.melosys.domain;

import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

@Entity
@Table(name = "utpekingsperiode")
public class Utpekingsperiode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

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

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandlingsresultat(Behandling behandling) {
        this.behandling = behandling;
    }

    public LocalDate getFom() {
        return fom;
    }

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
