package no.nav.melosys.domain;

import javax.persistence.*;

@Entity
@Table(name = "saksopplysning_kilde")
public class SaksopplysningKilde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksopplysning_id", nullable = false, updatable = false)
    public Saksopplysning saksopplysning;

    @Enumerated(EnumType.STRING)
    @Column(name = "kildesystem", nullable = false, updatable = false)
    private SaksopplysningKildesystem kilde;

    @Lob
    @Column(name = "mottatt_dokument", nullable = false)
    public String mottattDokument;

    public SaksopplysningKilde() {}

    public SaksopplysningKilde(Saksopplysning saksopplysning, SaksopplysningKildesystem kilde, String mottattDokument) {
        this.saksopplysning = saksopplysning;
        this.kilde = kilde;
        this.mottattDokument = mottattDokument;
    }
}
