package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.*;
import no.nav.melosys.domain.Behandlingsresultat;

@Entity
@Table(name = "aarsavregning")
public class Årsavregning {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "behandlingsresultat_id")
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "aar", nullable = false, updatable = false)
    private Integer aar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tidligere_resultat_id")
    private Behandlingsresultat tidligereBehandlingsresultat;

    @Column(name = "tidligere_fakturert_beloep")
    private BigDecimal tidligereFakturertBeloep;

    @Column(name = "nytt_totalbeloep")
    private BigDecimal nyttTotalbeloep;

    @Column(name = "til_fakturering_beloep")
    private BigDecimal tilFaktureringBeloep;

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

    public Integer getAar() {
        return aar;
    }

    public void setAar(Integer aar) {
        this.aar = aar;
    }

    public Behandlingsresultat getTidligereBehandlingsresultat() {
        return tidligereBehandlingsresultat;
    }

    public void setTidligereBehandlingsresultat(Behandlingsresultat tidligereBehandlingsresultat) {
        this.tidligereBehandlingsresultat = tidligereBehandlingsresultat;
    }

    public BigDecimal getTidligereFakturertBeloep() {
        return tidligereFakturertBeloep;
    }

    public void setTidligereFakturertBeloep(BigDecimal tidligereFakturertBeloep) {
        this.tidligereFakturertBeloep = tidligereFakturertBeloep;
    }

    public BigDecimal getNyttTotalbeloep() {
        return nyttTotalbeloep;
    }

    public void setNyttTotalbeloep(BigDecimal fastsattTotalbeloep) {
        this.nyttTotalbeloep = fastsattTotalbeloep;
    }

    public BigDecimal getTilFaktureringBeloep() {
        return tilFaktureringBeloep;
    }

    public void setTilFaktureringBeloep(BigDecimal tilFaktureringBeloep) {
        this.tilFaktureringBeloep = tilFaktureringBeloep;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Årsavregning that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(aar, that.aar) && Objects.equals(tidligereFakturertBeloep, that.tidligereFakturertBeloep) && Objects.equals(nyttTotalbeloep, that.nyttTotalbeloep) && Objects.equals(tilFaktureringBeloep, that.tilFaktureringBeloep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, aar, tidligereFakturertBeloep, nyttTotalbeloep, tilFaktureringBeloep);
    }

    public void beregnTilFaktureringsBeloep(){
        if (tidligereFakturertBeloep != null && nyttTotalbeloep != null) {
            tilFaktureringBeloep = nyttTotalbeloep.subtract(tidligereFakturertBeloep);
        }
    }
}
