package no.nav.melosys.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "VILKAAR_RESULTAT")
public class VilkaarsResultat {

    @Id
    private Long id;

    private String utfall; //TODO Francois

    private LocalDate startdato;

    private LocalDate sluttdato;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Regel> regler = new ArrayList<>();

    @OneToMany()
    private List<Saksopplysning> opplysninger = new ArrayList<>();

}
