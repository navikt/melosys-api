package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "BRUKER")
public class Bruker extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGen")
    @SequenceGenerator(name= "idGen", sequenceName = "SEQ_BRUKER")
    private Long id;

    private String navn;

    private Long fnr;

    @Column(name = "foedsel_dato")
    private LocalDate fødselsdato;

    public Long getId() {
        return id;
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
