package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "ARBEIDSFORHOLD")
public class Arbeidsforhold extends Saksopplysning {

    @Column(name = "arbeidsgiver_id")
    private Long arbeidsgiverId;

    @Column(name = "arbeidstaker_id")
    private Long arbeidstaker;

    @Column(name = "ansettelse_fra")
    private LocalDate ansettelseFra;

    @Column(name = "ansettelse_til")
    private LocalDate ansettelseTil;

    @ManyToOne
    @JoinColumn(name = "type")
    private ArbeidsforholdsType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "arbeidsforhold")
    List<Arbeidsavtale> arbeidsavtaleListe = new ArrayList<>();

    public List<Arbeidsavtale> getArbeidsavtaleListe() {
        return arbeidsavtaleListe;
    }

    public void setArbeidsavtaleListe(List<Arbeidsavtale> arbeidsavtaleListe) {
        this.arbeidsavtaleListe = arbeidsavtaleListe;
    }

    public Long getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public void setArbeidsgiverId(Long arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public Long getArbeidstaker() {
        return arbeidstaker;
    }

    public void setArbeidstaker(Long arbeidstaker) {
        this.arbeidstaker = arbeidstaker;
    }

    public LocalDate getAnsettelseFra() {
        return ansettelseFra;
    }

    public void setAnsettelseFra(LocalDate ansettelseFra) {
        this.ansettelseFra = ansettelseFra;
    }

    public LocalDate getAnsettelseTil() {
        return ansettelseTil;
    }

    public void setAnsettelseTil(LocalDate ansettelseTil) {
        this.ansettelseTil = ansettelseTil;
    }

    public ArbeidsforholdsType getType() {
        return type;
    }

    public void setType(ArbeidsforholdsType type) {
        this.type = type;
    }
}
