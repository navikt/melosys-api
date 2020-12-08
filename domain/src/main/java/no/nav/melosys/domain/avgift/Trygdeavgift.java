package no.nav.melosys.domain.avgift;

import javax.persistence.*;

import no.nav.melosys.domain.Medlemskapsperiode;

import static no.nav.melosys.domain.avgift.Trygdeavgift.AvgiftForInntekt.NORSK_INNTEKT;
import static no.nav.melosys.domain.avgift.Trygdeavgift.AvgiftForInntekt.UTENLANDSK_INNTEKT;

@Entity
@Table(name = "trygdeavgift")
public class Trygdeavgift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medlemskapsperiode_id")
    private Medlemskapsperiode medlemskapsperiode;

    @Column(name = "trygdeavgift_belop_md", nullable = false)
    private double trygdeavgiftsbeløpMd;

    @Column(name = "trygdesats", nullable = false)
    private double trygdesats;

    @Column(name = "avgiftskode", nullable = false)
    private String avgiftskode;

    @Column(name = "avgift_for_inntekt", nullable = false)
    @Enumerated(EnumType.STRING)
    private AvgiftForInntekt avgiftForInntekt;

    public Trygdeavgift() {
    }

    public Trygdeavgift(Medlemskapsperiode medlemskapsperiode,
                        double trygdeavgiftsbeløpMd,
                        double trygdesats,
                        String avgiftskode,
                        boolean erAvgiftForNorskInntekt) {
        this.medlemskapsperiode = medlemskapsperiode;
        this.trygdeavgiftsbeløpMd = trygdeavgiftsbeløpMd;
        this.trygdesats = trygdesats;
        this.avgiftskode = avgiftskode;
        this.avgiftForInntekt = erAvgiftForNorskInntekt ? NORSK_INNTEKT : UTENLANDSK_INNTEKT;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medlemskapsperiode getMedlemskapsperiode() {
        return medlemskapsperiode;
    }

    public void setMedlemskapsperiode(Medlemskapsperiode medlemskapsperiode) {
        this.medlemskapsperiode = medlemskapsperiode;
    }

    public double getTrygdeavgiftsbeløpMd() {
        return trygdeavgiftsbeløpMd;
    }

    public void setTrygdeavgiftsbeløpMd(double trygdeavgiftsBeløp) {
        this.trygdeavgiftsbeløpMd = trygdeavgiftsBeløp;
    }

    public double getTrygdesats() {
        return trygdesats;
    }

    public void setTrygdesats(double trygdesats) {
        this.trygdesats = trygdesats;
    }

    public String getAvgiftskode() {
        return avgiftskode;
    }

    public void setAvgiftskode(String avgiftskode) {
        this.avgiftskode = avgiftskode;
    }

    public AvgiftForInntekt getAvgiftForInntekt() {
        return avgiftForInntekt;
    }

    public void setAvgiftForInntekt(AvgiftForInntekt avgiftForInntekt) {
        this.avgiftForInntekt = avgiftForInntekt;
    }

    public boolean erAvgiftForNorskInntekt() {
        return avgiftForInntekt == NORSK_INNTEKT;
    }

    public enum AvgiftForInntekt {
        NORSK_INNTEKT, UTENLANDSK_INNTEKT
    }
}
