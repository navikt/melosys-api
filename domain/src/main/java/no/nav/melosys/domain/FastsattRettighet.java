package no.nav.melosys.domain;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "FASTSATT_RETTIGHET")
public class FastsattRettighet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGen")
    @SequenceGenerator(name= "idGen", sequenceName = "SEQ_RETTIGHET")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type", nullable = false)
    private RettighetsType type;

    private String lovvalgsland;

    @Column(name = "startdato", nullable = false)
    private LocalDate startdato;

    private LocalDate sluttdato;

    @Column(name = "standard_begrunnelse")
    private String standardBegrunnelse;

    @Column(name = "fritekst_begrunnelse")
    private String fritekstBegrunnelse;

    @OneToMany(cascade = CascadeType.ALL)
    List<VilkaarsResultat> vilkaarsResultatListe = new ArrayList<>();
}
