package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Objects;

@MappedSuperclass
public abstract class Kodeverk {

    @Id
    @Column(name = "kode", updatable = false, insertable = false)
    private String kode;

    @Column(name = "navn", insertable = false, updatable = false)
    private String navn;

    @Column(name = "beskrivelse", updatable = false, insertable = false)
    private String beskrivelse;

    protected Kodeverk() {
    }

    protected Kodeverk(String kode) {
        Objects.requireNonNull(kode);
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public String getNavn() {
        return navn;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FagsakStatus status = (FagsakStatus) o;
        return Objects.equals(kode, status.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode);
    }

}
