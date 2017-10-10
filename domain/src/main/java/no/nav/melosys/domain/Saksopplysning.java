package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ColumnTransformer;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;


@Entity
@Table(name = "saksopplysning")
public class Saksopplysning {

    // FIXME (farjam): Ikke tatt med fra den logiske modellen: opplysningsGyldighet, beskyttelsesbehov. betydning, registreringInfo og formål
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "opplysning_type", nullable = false, updatable = false)
    @Convert(converter = SaksopplysningType.DbKonverterer.class)
    private SaksopplysningType type;

    @Column(name="versjon", nullable = false, updatable = false)
    private String versjon;

    @Column(name = "kilde", nullable = false, updatable = false)
    @Convert(converter = SaksopplysningKilde.DbKonverterer.class)
    private SaksopplysningKilde kilde;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "dokument_xml", updatable = false, columnDefinition = "XMLType")
    @ColumnTransformer(read = "to_clob(dokument_xml)", write = "?")
    private String dokumentXml;
    
    @Transient
    private SaksopplysningDokument dokument;

    public long getId() {
        return id;
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

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public String getDokumentXml() {
        return dokumentXml;
    }

    public void setDokumentXml(String dokumentXml) {
        this.dokumentXml = dokumentXml;
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
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.behandling, that.behandling)
            && Objects.equals(this.registrertDato, that.registrertDato)
            && Objects.equals(this.dokumentXml, that.dokumentXml);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandling, registrertDato, dokumentXml);
    }

}
