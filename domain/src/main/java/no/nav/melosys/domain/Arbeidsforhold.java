package no.nav.melosys.domain;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
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


}
