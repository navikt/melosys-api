package no.nav.melosys.domain;

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
     * ID-basert equals - anbefalt JPA-mønster.
     * Unngår @Lob-felt som kan gi ustabil oppførsel mellom Hibernate-sesjoner.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaksopplysningKilde that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
