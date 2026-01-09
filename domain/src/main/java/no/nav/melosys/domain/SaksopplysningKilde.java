package no.nav.melosys.domain;

import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "saksopplysning_kilde")
@DynamicUpdate // Hindrer race condition mellom HTTP-tråd og saga-tråd - kun endrede kolonner inkluderes i UPDATE
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

    /**
     * Equals-implementasjon som følger JPA entity best practice.
     * Bruker id når tilgjengelig, ellers business key (saksopplysning + kilde).
     * VIKTIG: Ikke bruk @Lob-felt (mottattDokument) i equals/hashCode da CLOB-representasjon
     * kan variere mellom sesjoner og forårsake problemer med Set-medlemskap og orphanRemoval.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SaksopplysningKilde)) { // Implisitt nullsjekk
            return false;
        }
        SaksopplysningKilde that = (SaksopplysningKilde) o;
        if (this.id != null && that.id != null) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id.equals(that.id);
        }
        // Fallback til business key for upersisterte entiteter
        return Objects.equals(this.saksopplysning, that.saksopplysning)
            && Objects.equals(this.kilde, that.kilde);
    }

    /**
     * HashCode basert på uforanderlige felt for stabil Set-oppførsel.
     * Bruker kun kilde (enum) som er immutable etter opprettelse.
     */
    @Override
    public int hashCode() {
        return Objects.hash(kilde);
    }
}
