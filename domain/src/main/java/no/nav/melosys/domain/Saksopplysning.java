package no.nav.melosys.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.jpa.HibernateXmlType;
import no.nav.melosys.domain.jpa.SaksopplysningDokumentConverter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@TypeDefs(@TypeDef(name = Saksopplysning.XMLTYPE, typeClass = HibernateXmlType.class))
@Entity
@Table(name = "saksopplysning")
public class Saksopplysning {
    public static final String XMLTYPE = "xmltype";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Enumerated(EnumType.STRING)
    @Column(name = "opplysning_type", nullable = false, updatable = false)
    private SaksopplysningType type;

    @Column(name="versjon", nullable = false, updatable = false)
    private String versjon;

    @OneToMany(mappedBy = "saksopplysning", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<SaksopplysningKilde> kilder = new HashSet<>(1);

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false)
    private Instant endretDato;

    @Convert(converter = SaksopplysningDokumentConverter.class)
    @Column(name = "dokument", nullable = false, updatable = false)
    private SaksopplysningDokument dokument;

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

    public Set<SaksopplysningKilde> getKilder() {
        return kilder;
    }

    public void setKilder(Set<SaksopplysningKilde> kilder) {
        this.kilder = kilder;
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

    public SaksopplysningDokument getDokument() {
        return dokument;
    }

    public void setDokument(SaksopplysningDokument dokument) {
        this.dokument = dokument;
    }

    public void leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem kildesystem, String mottattDokument) {
        kilder.add(new SaksopplysningKilde(this, kildesystem, mottattDokument));
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
            && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandling, registrertDato, type);
    }

}
