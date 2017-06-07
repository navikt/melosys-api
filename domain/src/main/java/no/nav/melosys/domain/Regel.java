package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "REGEL")
public class Regel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type", nullable = false)
    private RegelType type;

    private String referanse;

    @Column(name = "forretning_versjon")
    private String forretningsVersjon;

    @Column(name = "teknisk_versjon")
    private String tekniskVersjon;

    @ManyToOne
    @JoinColumn(name = "vilkaar_resultat_id")
    private VilkaarsResultat vilkårsResultat;

    public RegelType getType() {
        return type;
    }

    public void setType(RegelType type) {
        this.type = type;
    }

    public String getReferanse() {
        return referanse;
    }

    public void setReferanse(String referanse) {
        this.referanse = referanse;
    }

    public String getForretningsVersjon() {
        return forretningsVersjon;
    }

    public void setForretningsVersjon(String forretningsVersjon) {
        this.forretningsVersjon = forretningsVersjon;
    }

    public String getTekniskVersjon() {
        return tekniskVersjon;
    }

    public void setTekniskVersjon(String tekniskVersjon) {
        this.tekniskVersjon = tekniskVersjon;
    }

    public VilkaarsResultat getVilkårsResultat() {
        return vilkårsResultat;
    }

    public void setVilkårsResultat(VilkaarsResultat vilkårsResultat) {
        this.vilkårsResultat = vilkårsResultat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Regel r = (Regel) o;
        return Objects.equals(type, r.getType())
            && Objects.equals(referanse, r.getReferanse())
            && Objects.equals(forretningsVersjon, r.getForretningsVersjon())
            && Objects.equals(tekniskVersjon, r.getTekniskVersjon())                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, referanse, forretningsVersjon, tekniskVersjon);
    }
}
