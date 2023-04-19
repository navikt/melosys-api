package no.nav.melosys.domain.folketrygden;

import javax.persistence.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlaget;
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer;

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
    private Trygdeavgiftsgrunnlaget trygdeavgiftsgrunnlaget;

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

    public Trygdeavgiftsgrunnlaget getTrygdeavgiftsgrunnlaget() {
        return trygdeavgiftsgrunnlaget;
    }

    public void setTrygdeavgiftsgrunnlaget(Trygdeavgiftsgrunnlaget trygdeavgiftsgrunnlaget) {
        this.trygdeavgiftsgrunnlaget = trygdeavgiftsgrunnlaget;
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
}
