package no.nav.melosys.saksflyt.impl.domain;

import java.time.LocalDate;

import javax.persistence.Id;

import no.nav.melosys.saksflyt.api.Sak;
import no.nav.melosys.saksflyt.api.Status;

/**
 * FIXME (farjam): Avventer domenemodellen
 * 
 * @author m126664
 *
 */

public class SakImpl implements Sak {

    @Id
    private long saksId;

    private Status status;

    private LocalDate registrertDato;

    private LocalDate fristDato;

    @Override
    public long getSaksId() {
        return saksId;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public LocalDate getFristDato() {
        return fristDato;
    }

    public void setFristDato(LocalDate fristDato) {
        this.fristDato = fristDato;
    }

    @Override
    public LocalDate getRegistrertDato() {
        return registrertDato;
    }

    @Override
    public void leggTilMerknad(String Merknad) {
        // FIXME (Farjam): Avventer dbmodell
    }

}
