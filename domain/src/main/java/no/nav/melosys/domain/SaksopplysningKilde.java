package no.nav.melosys.domain;

import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "saksopplysning_kilde")
public class SaksopplysningKilde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksopplysning_id", nullable = false, updatable = false)
    private Saksopplysning saksopplysning;

    @Enumerated(EnumType.STRING)
    @Column(name = "kildesystem", nullable = false, updatable = false)
    private SaksopplysningKildesystem kilde;

    @Lob
    @Column(name = "mottatt_dokument", nullable = false)
    private String mottattDokument;

    public SaksopplysningKilde() {}

    public SaksopplysningKilde(Saksopplysning saksopplysning, SaksopplysningKildesystem kilde, String mottattDokument) {
        this.saksopplysning = saksopplysning;
        this.kilde = kilde;
        this.mottattDokument = mottattDokument;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Saksopplysning getSaksopplysning() {
        return saksopplysning;
    }

    public void setSaksopplysning(Saksopplysning saksopplysning) {
        this.saksopplysning = saksopplysning;
    }

    public SaksopplysningKildesystem getKilde() {
        return kilde;
    }

    public void setKilde(SaksopplysningKildesystem kilde) {
        this.kilde = kilde;
    }

    public String getMottattDokument() {
        return mottattDokument;
    }

    public void setMottattDokument(String mottattDokument) {
        this.mottattDokument = mottattDokument;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SaksopplysningKilde that = (SaksopplysningKilde) o;
        return Objects.equals(this.kilde, that.kilde)
            && Objects.equals(this.mottattDokument, that.mottattDokument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kilde, mottattDokument);
    }
}
