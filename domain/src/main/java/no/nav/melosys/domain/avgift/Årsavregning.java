package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.*;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg;

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

    @Column(name = "beregnet_avgift_belop")
    private BigDecimal beregnetAvgiftBelop;

    @Column(name = "til_fakturering_beloep")
    private BigDecimal tilFaktureringBeloep;

    @Column(name = "har_data_fra_avgiftssystemet")
    private Boolean harDeltGrunnlag;

    @Column(name = "har_avvik")
    private Boolean harAvvik;

    @Column(name = "tidligere_fakturert_beloep_avgiftssystem")
    private BigDecimal tidligereFakturertBeloepAvgiftssystem;

    @Column(name = "manuelt_avgift_beloep")
    private BigDecimal manueltAvgiftBeloep;

    @Enumerated(EnumType.STRING)
    @Column(name = "endelig_avgift_valg")
    private EndeligAvgiftValg endeligAvgiftValg;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Årsavregning that = (Årsavregning) o;
        return Objects.equals(id, that.id) && Objects.equals(behandlingsresultat, that.behandlingsresultat) && Objects.equals(aar, that.aar) && Objects.equals(tidligereBehandlingsresultat, that.tidligereBehandlingsresultat) && Objects.equals(tidligereFakturertBeloep, that.tidligereFakturertBeloep) && Objects.equals(beregnetAvgiftBelop, that.beregnetAvgiftBelop) && Objects.equals(tilFaktureringBeloep, that.tilFaktureringBeloep) && Objects.equals(harDeltGrunnlag, that.harDeltGrunnlag) && Objects.equals(harAvvik, that.harAvvik) && Objects.equals(tidligereFakturertBeloepAvgiftssystem, that.tidligereFakturertBeloepAvgiftssystem) && Objects.equals(manueltAvgiftBeloep, that.manueltAvgiftBeloep) && endeligAvgiftValg == that.endeligAvgiftValg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingsresultat, aar, tidligereBehandlingsresultat, tidligereFakturertBeloep, beregnetAvgiftBelop, tilFaktureringBeloep, harDeltGrunnlag, harAvvik, tidligereFakturertBeloepAvgiftssystem, manueltAvgiftBeloep, endeligAvgiftValg);
    }

    public BigDecimal getManueltAvgiftBeloep() {
        return manueltAvgiftBeloep;
    }

    public void setManueltAvgiftBeloep(BigDecimal manueltAvgiftBeloep) {
        this.manueltAvgiftBeloep = manueltAvgiftBeloep;
    }

    public EndeligAvgiftValg getEndeligAvgiftValg() {
        return endeligAvgiftValg;
    }

    public void setEndeligAvgiftValg(EndeligAvgiftValg endeligAvgift) {
        this.endeligAvgiftValg = endeligAvgift;
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

    public BigDecimal getBeregnetAvgiftBelop() {
        return beregnetAvgiftBelop;
    }

    public void setBeregnetAvgiftBelop(BigDecimal fastsattTotalbeloep) {
        this.beregnetAvgiftBelop = fastsattTotalbeloep;
    }

    public BigDecimal getTilFaktureringBeloep() {
        return tilFaktureringBeloep;
    }

    public void setTilFaktureringBeloep(BigDecimal tilFaktureringBeloep) {
        this.tilFaktureringBeloep = tilFaktureringBeloep;
    }

    // TODO: Legg inn unntak for 25 % regel
    public void beregnTilFaktureringsBeloep() {
        if (beregnetAvgiftBelop == null && manueltAvgiftBeloep == null) return;

        tilFaktureringBeloep = (manueltAvgiftBeloep != null ? manueltAvgiftBeloep : beregnetAvgiftBelop)
            .subtract(tidligereFakturertBeloep != null ? tidligereFakturertBeloep : BigDecimal.ZERO)
            .subtract(tidligereFakturertBeloepAvgiftssystem != null ? tidligereFakturertBeloepAvgiftssystem : BigDecimal.ZERO)
            .add(tidligereFakturertBelopAvgiftssytemForrigeÅrsavregning());
    }

    private BigDecimal tidligereFakturertBelopAvgiftssytemForrigeÅrsavregning() {
        return tidligereBehandlingsresultat != null && tidligereBehandlingsresultat.getårsavregning() != null
            ? tidligereBehandlingsresultat.getårsavregning().getTidligereFakturertBeloepAvgiftssystem()
            : BigDecimal.ZERO;
    }
}
