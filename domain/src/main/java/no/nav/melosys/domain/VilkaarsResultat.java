package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "VILKAAR_RESULTAT")
public class VilkaarsResultat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utfall")
    private VilkaarsResultatUtfallType utfall;

    private LocalDate startdato;

    private LocalDate sluttdato;

    @ManyToOne
    @JoinColumn(name = "rettighet_id")
    private FastsattRettighet rettighet;

    @ManyToMany
    @JoinTable(name = "BEHANDLING_GRUNNLAG",
            joinColumns=@JoinColumn(name="vilkaar_resultat_id"),
            inverseJoinColumns=@JoinColumn(name="saksopplysning_id"))
    private List<Saksopplysning> opplysninger = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "vilkårsResultat")
    private List<Regel> regler = new ArrayList<>();

    public VilkaarsResultatUtfallType getUtfall() {
        return utfall;
    }

    public void setUtfall(VilkaarsResultatUtfallType utfall) {
        this.utfall = utfall;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    public void setStartdato(LocalDate startdato) {
        this.startdato = startdato;
    }

    public LocalDate getSluttdato() {
        return sluttdato;
    }

    public void setSluttdato(LocalDate sluttdato) {
        this.sluttdato = sluttdato;
    }

    public FastsattRettighet getRettighet() {
        return rettighet;
    }

    public void setRettighet(FastsattRettighet rettighet) {
        this.rettighet = rettighet;
    }

    public List<Saksopplysning> getOpplysninger() {
        return opplysninger;
    }

    public void setOpplysninger(List<Saksopplysning> opplysninger) {
        this.opplysninger = opplysninger;
    }

    public List<Regel> getRegler() {
        return regler;
    }

    public void setRegler(List<Regel> regler) {
        this.regler = regler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VilkaarsResultat v = (VilkaarsResultat) o;
        return Objects.equals(rettighet, v.getRettighet())
            && Objects.equals(utfall, v.getUtfall())
            && Objects.equals(startdato, v.getStartdato())
            && Objects.equals(sluttdato, v.getSluttdato())
            && Objects.equals(regler, v.getRegler());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rettighet, utfall, startdato, sluttdato, regler);
    }
}
