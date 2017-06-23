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

    @ManyToOne
    @JoinColumn(name = "arbeidsgiver_id", nullable = false)
    private Arbeidsgiver arbeidsgiver;

    @ManyToOne
    @JoinColumn(name = "arbeidstaker_id")
    private Bruker arbeidstaker;

    @Column(name = "ansettelse_fra")
    private LocalDate ansettelseFra;

    @Column(name = "ansettelse_til")
    private LocalDate ansettelseTil;

    @Column(name = "sist_bekreftet")
    private LocalDate sistBekreftet;

    @ManyToOne
    @JoinColumn(name = "type")
    private ArbeidsforholdsType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "arbeidsforhold")
    List<Arbeidsavtale> arbeidsavtaleListe = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "arbeidsforhold")
    List<PermisjonOgPermittering> permisjonOgPermitteringListe = new ArrayList<>();

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public Bruker getArbeidstaker() {
        return arbeidstaker;
    }

    public void setArbeidstaker(Bruker arbeidstaker) {
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

    public LocalDate getSistBekreftet() {
        return sistBekreftet;
    }

    public void setSistBekreftet(LocalDate sistBekreftet) {
        this.sistBekreftet = sistBekreftet;
    }

    public ArbeidsforholdsType getType() {
        return type;
    }

    public void setType(ArbeidsforholdsType type) {
        this.type = type;
    }

    public List<Arbeidsavtale> getArbeidsavtaleListe() {
        return arbeidsavtaleListe;
    }

    public void setArbeidsavtaleListe(List<Arbeidsavtale> arbeidsavtaleListe) {
        this.arbeidsavtaleListe = arbeidsavtaleListe;
    }
}
