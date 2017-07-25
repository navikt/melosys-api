package no.nav.melosys.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "PERMISJON")
public class PermisjonOgPermittering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "arbeidsforhold_id")
    private Arbeidsforhold arbeidsforhold;

    @Column(name = "endret")
    private LocalDateTime endringsTidspunkt;

    @Column(name = "permisjon_id")
    private String permisjonsId;

    @Column
    private BigDecimal prosent;

    @Column(name = "startdato")
    private LocalDate startDato;

    @Column(name = "sluttdato")
    private LocalDate sluttDato;

    public String getPermisjonsId() {
        return permisjonsId;
    }

    public void setPermisjonsId(String permisjonsId) {
        this.permisjonsId = permisjonsId;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public LocalDateTime getEndringsTidspunkt() {
        return endringsTidspunkt;
    }

    public void setEndringsTidspunkt(LocalDateTime endringsTidspunkt) {
        this.endringsTidspunkt = endringsTidspunkt;
    }

    public BigDecimal getProsent() {
        return prosent;
    }

    public void setProsent(BigDecimal prosent) {
        this.prosent = prosent;
    }

    public LocalDate getStartDato() {
        return startDato;
    }

    public void setStartDato(LocalDate startDato) {
        this.startDato = startDato;
    }

    public LocalDate getSluttDato() {
        return sluttDato;
    }

    public void setSluttDato(LocalDate sluttDato) {
        this.sluttDato = sluttDato;
    }

}
