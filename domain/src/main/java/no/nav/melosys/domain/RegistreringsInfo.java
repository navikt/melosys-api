package no.nav.melosys.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@MappedSuperclass
public class RegistreringsInfo {

    @CreatedDate
    @Column(name = "registrert_dato", nullable = false, updatable = false)
    Instant registrertDato;

    @CreatedBy
    @Column(name = "registrert_av", nullable = false)
    private String registrertAv;

    @LastModifiedDate
    @Column(name = "endret_dato", nullable = false)
    private Instant endretDato;

    @LastModifiedBy
    @Column(name = "endret_av", nullable = false)
    private String endretAv;

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public String getRegistrertAv() {
        return registrertAv;
    }

    public void setRegistrertAv(String registrertAv) {
        this.registrertAv = registrertAv;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public void setEndretAv(String endretAv) {
        this.endretAv = endretAv;
    }
}
