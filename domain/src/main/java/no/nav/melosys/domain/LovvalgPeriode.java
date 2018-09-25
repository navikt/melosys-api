package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.dokument.vurdering.VurderingGrunnlag;
import no.nav.melosys.domain.dokument.vurdering.VurderingResultat;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "lovvalg_periode")
public class LovvalgPeriode implements ErPeriode {
    
    // FIXME: Ikke tatt med fra den logiske modellen: lovvalgsBestemmelseBegrunnelse
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vedtak_id", nullable = false, updatable = false)
    private Vedtak vedtak;
    
    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fom;

    @Column(name = "tom_dato", nullable = false, updatable = false)
    private LocalDate tom;

    @Column(name = "bestemmelse", nullable = false, updatable = false)
    private String bestemmelse;
    
    /** Kodeverk: no.nav.melosys.service.kodeverk.Kodeverk.LANDKODER */
    @Column(name = "lovvalgsland", nullable = false, updatable = false)
    private String Land;
    
    @Column(name = "dekning", nullable = false, updatable = false)
    @Convert(converter = LovvalgDekning.DbKonverterer.class)
    private LovvalgDekning dekning;
    
    @Column(name="versjon", nullable = false, updatable = false)
    private String versjon;
    
    @Column(name = "grunnlag_xml", updatable = false, columnDefinition = "XMLType")
    @ColumnTransformer(read = "to_clob(dokument_xml)", write = "?")
    private String grunnlagXml;

    @Column(name = "resultat_xml", updatable = false, columnDefinition = "XMLType")
    @ColumnTransformer(read = "to_clob(dokument_xml)", write = "?")
    private String resultatXml;

    @OneToMany(mappedBy = "lovvalgPeriode", fetch = FetchType.EAGER)
    private Set<Faktagrunnlag> faktagrunnlag;
    
    @Transient
    private VurderingGrunnlag grunnlagDokument;
    
    @Transient
    private VurderingResultat resultatDokument;

    public long getId() {
        return id;
    }

    public Vedtak getVedtak() {
        return vedtak;
    }

    public void setVedtak(Vedtak vedtak) {
        this.vedtak = vedtak;
    }

    @Override
    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    @Override
    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(String bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public String getLand() {
        return Land;
    }

    public void setLand(String land) {
        Land = land;
    }

    public LovvalgDekning getDekning() {
        return dekning;
    }

    public void setDekning(LovvalgDekning dekning) {
        this.dekning = dekning;
    }

    public String getVersjon() {
        return versjon;
    }

    public void setVersjon(String versjon) {
        this.versjon = versjon;
    }

    public String getGrunnlagXml() {
        return grunnlagXml;
    }

    public void setGrunnlagXml(String grunnlagXml) {
        this.grunnlagXml = grunnlagXml;
    }

    public String getResultatXml() {
        return resultatXml;
    }

    public void setResultatXml(String resultatXml) {
        this.resultatXml = resultatXml;
    }

    public Set<Faktagrunnlag> getFaktagrunnlag() {
        return faktagrunnlag;
    }

    public void setFaktagrunnlag(Set<Faktagrunnlag> faktagrunnlag) {
        this.faktagrunnlag = faktagrunnlag;
    }

    public VurderingGrunnlag getGrunnlagDokument() {
        return grunnlagDokument;
    }

    public void setGrunnlagDokument(VurderingGrunnlag grunnlagDokument) {
        this.grunnlagDokument = grunnlagDokument;
    }

    public VurderingResultat getResultatDokument() {
        return resultatDokument;
    }

    public void setResultatDokument(VurderingResultat resultatDokument) {
        this.resultatDokument = resultatDokument;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LovvalgPeriode)) {
            return false;
        }
        LovvalgPeriode that = (LovvalgPeriode) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.vedtak, that.vedtak)
            && Objects.equals(this.fom, that.fom);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(vedtak, fom);
    }

}
