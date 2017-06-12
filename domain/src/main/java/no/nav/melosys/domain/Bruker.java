package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "BRUKER")
public class Bruker extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String navn;

    private String fnr;

    @ManyToOne
    @JoinColumn(name = "bruker_kjoenn", nullable = false)
    private Kjoenn kjønn;

    @Column(name = "foedsel_dato")
    private LocalDate fødselsdato;

    public Long getId() {
        return id;
    }

    public Kjoenn getKjønn() {
        return kjønn;
    }

    public void setKjønn(Kjoenn kjønn) {
        this.kjønn = kjønn;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Bruker bruker = (Bruker) o;
        return Objects.equals(getAktørId(), bruker.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAktørId());
    }
}
