package no.nav.melosys.domain;

import javax.persistence.*;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "personopplysning_kilde")
public class PersonopplysningKilde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "personopplysning_id", nullable = false, updatable = false)
    public Personopplysning personopplysning;

    @Type(type = "xmltype")
    @Column(name = "dokument_xml", nullable = false)
    public String dokumentXml;

    public PersonopplysningKilde() {
    }

    public PersonopplysningKilde(Personopplysning personopplysning, String dokumentXml) {
        this.personopplysning = personopplysning;
        this.dokumentXml = dokumentXml;
    }
}
