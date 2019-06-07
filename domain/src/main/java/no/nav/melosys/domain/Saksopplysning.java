package no.nav.melosys.domain;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.jpa.SaksopplysningListener;
import org.hibernate.annotations.ColumnTransformer;


@Entity
@EntityListeners({SaksopplysningListener.class})
@Table(name = "saksopplysning")
public class Saksopplysning {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "opplysning_type", nullable = false, updatable = false)
    @Convert(converter = SaksopplysningType.DbConverter.class)
    private SaksopplysningType type;

    @Column(name="versjon", nullable = false, updatable = false)
    private String versjon;

    @Column(name = "kilde", nullable = false, updatable = false)
    @Convert(converter = SaksopplysningKilde.DbKonverterer.class)
    private SaksopplysningKilde kilde;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false)
    private Instant endretDato;

    @ColumnTransformer(read = "NVL2(dokument_xml, (dokument_xml).getClobVal(), NULL)", write = "XMLType.createxml(?)")
    @Lob
    @Column(name = "dokument_xml", columnDefinition = "XMLType")
    private String dokumentXml;

    @ColumnTransformer(read = "NVL2(intern_xml, (intern_xml).getClobVal(), NULL)", write = "XMLType.createxml(?)")
    @Lob
    @Column(name = "intern_xml", columnDefinition = "XMLType")
    private String internXml;

    @Transient
    private SaksopplysningDokument dokument;

    public Saksopplysning() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public SaksopplysningType getType() {
        return type;
    }

    public void setType(SaksopplysningType type) {
        this.type = type;
    }

    public String getVersjon() {
        return versjon;
    }

    public void setVersjon(String versjon) {
        this.versjon = versjon;
    }

    public SaksopplysningKilde getKilde() {
        return kilde;
    }

    public void setKilde(SaksopplysningKilde kilde) {
        this.kilde = kilde;
    }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public String getDokumentXml() {
        return dokumentXml;
    }

    public void setDokumentXml(String dokumentXml) {
        this.dokumentXml = dokumentXml;
    }

    public String getInternXml() {
        return internXml;
    }

    public void setInternXml(String internXml) {
        this.internXml = internXml;
    }

    public SaksopplysningDokument getDokument() {
        return dokument;
    }

    public void setDokument(SaksopplysningDokument dokument) {
        this.dokument = dokument;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Saksopplysning)) { // Implisitt nullsjekk
            return false;
        }
        Saksopplysning that = (Saksopplysning) o;
        if (this.id != null && that.id != null) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id.equals(that.getId());
        }
        return Objects.equals(this.behandling, that.behandling)
            && Objects.equals(this.registrertDato, that.registrertDato)
            && Objects.equals(this.type, that.type)
            && Objects.equals(this.kilde, that.kilde)
            && Objects.equals(this.dokumentXml, that.dokumentXml);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandling, registrertDato, type, kilde, dokumentXml);
    }

}
