package no.nav.melosys.domain.folketrygden;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.avgift.Inntektsperiode;
import no.nav.melosys.domain.avgift.Trygdeavgift;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer;
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker;

@Entity
@Table(name = "fastsatt_trygdeavgift")
public class FastsattTrygdeavgift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "medlem_av_folketrygden_id", nullable = false, updatable = false)
    private MedlemAvFolketrygden medlemAvFolketrygden;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private Trygdeavgift_typer trygdeavgiftstype;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "betales_av")
    private Aktoer betalesAv;

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Column(name = "representant_nr")
    private String representantNr;

    @OneToOne(mappedBy = "fastsattTrygdeavgift", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag;

    @OneToMany(mappedBy = "fastsattTrygdeavgift", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Trygdeavgift> trygdeavgift = new HashSet<>(1);

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Column(name = "avgiftspliktig_norsk_inntekt_md")
    private Long avgiftspliktigNorskInntektMnd;

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Column(name = "avgiftspliktig_utenlandsk_inntekt_md")
    private Long avgiftspliktigUtenlandskInntektMnd;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MedlemAvFolketrygden getMedlemAvFolketrygden() {
        return medlemAvFolketrygden;
    }

    public void setMedlemAvFolketrygden(MedlemAvFolketrygden medlemAvFolketrygden) {
        this.medlemAvFolketrygden = medlemAvFolketrygden;
    }

    public Trygdeavgift_typer getTrygdeavgiftstype() {
        return trygdeavgiftstype;
    }

    public void setTrygdeavgiftstype(Trygdeavgift_typer trygdeavgiftstype) {
        this.trygdeavgiftstype = trygdeavgiftstype;
    }

    public Aktoer getBetalesAv() {
        return betalesAv;
    }

    public void setBetalesAv(Aktoer betalesAv) {
        this.betalesAv = betalesAv;
    }

    public String getRepresentantNr() {
        return representantNr;
    }

    public void setRepresentantNr(String representantNr) {
        this.representantNr = representantNr;
    }

    public Trygdeavgiftsgrunnlag getTrygdeavgiftsgrunnlag() {
        return trygdeavgiftsgrunnlag;
    }

    public void setTrygdeavgiftsgrunnlag(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag) {
        trygdeavgiftsgrunnlag.setFastsattTrygdeavgift(this);
        this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag;
    }

    public Long getAvgiftspliktigNorskInntektMnd() {
        return avgiftspliktigNorskInntektMnd;
    }

    public void setAvgiftspliktigNorskInntektMnd(Long avgiftspliktigNorskInntektMnd) {
        this.avgiftspliktigNorskInntektMnd = avgiftspliktigNorskInntektMnd;
    }

    public Long getAvgiftspliktigUtenlandskInntektMnd() {
        return avgiftspliktigUtenlandskInntektMnd;
    }

    public void setAvgiftspliktigUtenlandskInntektMnd(Long avgiftspliktigUtenlandskInntektMnd) {
        this.avgiftspliktigUtenlandskInntektMnd = avgiftspliktigUtenlandskInntektMnd;
    }

    public Set<Trygdeavgift> getTrygdeavgift() {
        return trygdeavgift;
    }

    public void setTrygdeavgift(Set<Trygdeavgift> trygdeavgift) {
        this.trygdeavgift = trygdeavgift;
    }

    public Trygdeavgiftmottaker getTrygdeavgiftMottaker() {
        var inntektsperioder = trygdeavgiftsgrunnlag.getInntektsperioder();

        if (inntektsperioder.stream().map(Inntektsperiode::isTrygdeavgiftBetalesTilSkatt).distinct().count() != 1) {
            return Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT;
        }
        return inntektsperioder.iterator().next().isTrygdeavgiftBetalesTilSkatt()
            ? Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
            : Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV;
    }

    public boolean skalBetaleTrygdeavgiftTilNav() {
        return getTrygdeavgiftMottaker() != Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT;
    }
}
