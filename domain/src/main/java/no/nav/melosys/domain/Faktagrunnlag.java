package no.nav.melosys.domain;

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

@Entity
@Table(name = "faktagrunnlag")
public class Faktagrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lovvalgperiode_id", nullable = false, updatable = false)
    private LovvalgPeriode lovvalgPeriode;

    @ManyToOne(optional = false)
    @JoinColumn(name="saksopplysning_id", nullable = false, updatable = false)
    private Saksopplysning saksopplysning;
    
    @Column(name = "type", nullable = false, updatable = false)
    @Convert(converter = FaktagrunnlagType.DbKonverterer.class)
    private FaktagrunnlagType type;

    public long getId() {
        return id;
    }

    public LovvalgPeriode getLovvalgPeriode() {
        return lovvalgPeriode;
    }

    public void setLovvalgPeriode(LovvalgPeriode lovvalgPeriode) {
        this.lovvalgPeriode = lovvalgPeriode;
    }

    public Saksopplysning getSaksopplysning() {
        return saksopplysning;
    }

    public void setSaksopplysning(Saksopplysning saksopplysning) {
        this.saksopplysning = saksopplysning;
    }

    public FaktagrunnlagType getType() {
        return type;
    }

    public void setType(FaktagrunnlagType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Faktagrunnlag)) {
            return false;
        }
        Faktagrunnlag that = (Faktagrunnlag) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.lovvalgPeriode, that.lovvalgPeriode)
            && Objects.equals(this.saksopplysning, that.saksopplysning)
            && Objects.equals(this.type, that.type);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(lovvalgPeriode, saksopplysning, type);
    }
    
}
