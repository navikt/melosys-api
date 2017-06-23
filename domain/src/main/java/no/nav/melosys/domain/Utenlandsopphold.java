package no.nav.melosys.domain;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "UTENLANDSOPPHOLD")
public class Utenlandsopphold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "arbeidsforhold_id")
    private Arbeidsforhold arbeidsforhold;

    @Column
    private String land;

    @Column(name = "startdato")
    private LocalDate startdato;

    @Column(name = "sluttdato")
    private LocalDate sluttdato;

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    public void setStartdato(LocalDate startdato) {
        this.startdato = startdato;
    }

    public LocalDate getSluttdato() {
        return sluttdato;
    }

    public void setSluttdato(LocalDate sluttdato) {
        this.sluttdato = sluttdato;
    }
}
