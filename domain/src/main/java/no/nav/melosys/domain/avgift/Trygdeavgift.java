package no.nav.melosys.domain.avgift;

import javax.persistence.*;

import no.nav.melosys.domain.Medlemskapsperiode;

@Entity
@Table(name = "trygdeavgift")
public class Trygdeavgift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medlemskapsperiode_id")
    private Medlemskapsperiode medlemskapsperiode;

    @Column(name = "trygdesats", nullable = false)
    private Double trygdesats;

    @Column(name = "avgiftskode", nullable = false)
    private String avgiftskode;

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

    public Double getTrygdesats() {
        return trygdesats;
    }

    public void setTrygdesats(Double trygdesats) {
        this.trygdesats = trygdesats;
    }

    public String getAvgiftskode() {
        return avgiftskode;
    }

    public void setAvgiftskode(String avgiftskode) {
        this.avgiftskode = avgiftskode;
    }
}
