package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;

@Entity
@Table(name = "aarsavregning")
public class Aarsavregning {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @MapsId
    @JoinColumn(name = "behandlingsresultat_id")
    private Behandlingsresultat behandlingsresultat;

    @ManyToOne()
    @JoinColumn(name = "tidligere_behandling_id")
    private Behandling tidligereBehandling;

    @Column(name = "aar")
    private Integer aar;

    @Column(name = "tidligere_fakturert_beloep")
    private BigDecimal tidligereFakturertBeloep;

    @Column(name = "fastsatt_totalbeloep")
    private BigDecimal fastsattTotalbeloep;

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

    public Behandling getTidligereBehandling() {
        return tidligereBehandling;
    }

    public void setTidligereBehandling(Behandling tidligereBehandling) {
        this.tidligereBehandling = tidligereBehandling;
    }

    public Integer getAar() {
        return aar;
    }

    public void setAar(Integer aar) {
        this.aar = aar;
    }

    public BigDecimal getTidligereFakturertBeloep() {
        return tidligereFakturertBeloep;
    }

    public void setTidligereFakturertBeloep(BigDecimal tidligereFakturertBeloep) {
        this.tidligereFakturertBeloep = tidligereFakturertBeloep;
    }

    public BigDecimal getFastsattTotalbeloep() {
        return fastsattTotalbeloep;
    }

    public void setFastsattTotalbeloep(BigDecimal fastsattTotalbeloep) {
        this.fastsattTotalbeloep = fastsattTotalbeloep;
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
        if (!(o instanceof Aarsavregning that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(aar, that.aar) && Objects.equals(tidligereFakturertBeloep, that.tidligereFakturertBeloep) && Objects.equals(fastsattTotalbeloep, that.fastsattTotalbeloep) && Objects.equals(tilFaktureringBeloep, that.tilFaktureringBeloep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, aar, tidligereFakturertBeloep, fastsattTotalbeloep, tilFaktureringBeloep);
    }
}
