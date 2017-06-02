package no.nav.melosys.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * EESSI2-35: Eksempel på bruk av JPA/Hibernate
 * TODO (Francois) Fjernes når domenemodellen kommer
 */

@Entity
@Table(name="PERSON")
public class PersonEks {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PERSON_ID_GEN")
    @SequenceGenerator(name = "PERSON_ID_GEN", sequenceName = "SEQ_PERSON", allocationSize = 1)
    Long id;

    String navn;

    String email;

    @Column(name = "OPPRETTET")
    Date date;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}