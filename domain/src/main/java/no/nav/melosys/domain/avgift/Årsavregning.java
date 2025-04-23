package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.*;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.AarsavregningBehandlingsvalg;

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

    @Column(name = "har_data_fra_avgiftssystemet")
    private Boolean harDeltGrunnlag;

    @Column(name = "har_avvik")
    private Boolean harAvvik;

    @Column(name = "tidligere_fakturert_beloep_avgiftssystem")
    private BigDecimal tidligereFakturertBeloepAvgiftssystem;

    @Column(name = "avgift_25_prosent")
    private BigDecimal avgift25Prosent;

    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingsvalg")
    private AarsavregningBehandlingsvalg behandlingsvalg;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Årsavregning that = (Årsavregning) o;
        return Objects.equals(id, that.id) && Objects.equals(behandlingsresultat, that.behandlingsresultat) && Objects.equals(aar, that.aar) && Objects.equals(tidligereBehandlingsresultat, that.tidligereBehandlingsresultat) && Objects.equals(tidligereFakturertBeloep, that.tidligereFakturertBeloep) && Objects.equals(nyttTotalbeloep, that.nyttTotalbeloep) && Objects.equals(tilFaktureringBeloep, that.tilFaktureringBeloep) && Objects.equals(harDeltGrunnlag, that.harDeltGrunnlag) && Objects.equals(harAvvik, that.harAvvik) && Objects.equals(tidligereFakturertBeloepAvgiftssystem, that.tidligereFakturertBeloepAvgiftssystem) && Objects.equals(avgift25Prosent, that.avgift25Prosent) && behandlingsvalg == that.behandlingsvalg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingsresultat, aar, tidligereBehandlingsresultat, tidligereFakturertBeloep, nyttTotalbeloep, tilFaktureringBeloep, harDeltGrunnlag, harAvvik, tidligereFakturertBeloepAvgiftssystem, avgift25Prosent, behandlingsvalg);
    }

    public BigDecimal getAvgift25Prosent() {
        return avgift25Prosent;
    }

    public void setAvgift25Prosent(BigDecimal avgift25Prosent) {
        this.avgift25Prosent = avgift25Prosent;
    }

    public AarsavregningBehandlingsvalg getBehandlingsvalg() {
        return behandlingsvalg;
    }

    public void setBehandlingsvalg(AarsavregningBehandlingsvalg behandlingsvalg) {
        this.behandlingsvalg = behandlingsvalg;
    }

    public BigDecimal getTidligereFakturertBeloepAvgiftssystem() {
        return tidligereFakturertBeloepAvgiftssystem;
    }

    public void setTidligereFakturertBeloepAvgiftssystem(BigDecimal tidligereFakturertBeloepAvgiftssytem) {
        this.tidligereFakturertBeloepAvgiftssystem = tidligereFakturertBeloepAvgiftssytem;
    }

    public Boolean getHarDeltGrunnlag() {
        return harDeltGrunnlag;
    }

    public void setHarDeltGrunnlag(Boolean harDeltGrunnlag) {
        this.harDeltGrunnlag = harDeltGrunnlag;
    }

    public Boolean getHarAvvik() {
        return harAvvik;
    }

    public void setHarAvvik(Boolean harAvvik) {
        this.harAvvik = harAvvik;
    }

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

    // TODO: Legg inn unntak for 25 % regel
    public void beregnTilFaktureringsBeloep() {
        if (nyttTotalbeloep == null) return;

        tilFaktureringBeloep = nyttTotalbeloep
            .subtract(tidligereFakturertBeloep != null ? tidligereFakturertBeloep : BigDecimal.ZERO)
            .subtract(harDeltGrunnlag != null && tidligereFakturertBeloepAvgiftssystem != null ? tidligereFakturertBeloepAvgiftssystem : BigDecimal.ZERO);
    }
}
