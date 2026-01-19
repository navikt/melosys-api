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

    @Version
    @Column(name = "versjon")
    private Long versjon;

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
     * Equals implementation that avoids using @Lob field (mottattDokument) to ensure stable Set behavior.
     * <p>
     * Using @Lob in equals/hashCode causes issues because CLOB representations can vary between
     * Hibernate sessions, leading to unstable hashCode values and incorrect Set membership detection.
     * This can cause OptimisticLockingException even without actual concurrent modification.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SaksopplysningKilde that)) {
            return false;
        }
        // Persisted entities: compare by id only
        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }
        // Unpersisted: use business key WITHOUT @Lob field
        return Objects.equals(this.saksopplysning, that.saksopplysning)
            && Objects.equals(this.kilde, that.kilde);
    }

    @Override
    public int hashCode() {
        // Only use immutable field - stable across Hibernate sessions
        return Objects.hash(kilde);
    }
}
