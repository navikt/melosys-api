package no.nav.melosys.domain;

import javax.persistence.*;

import no.nav.melosys.domain.jpa.HibernateXmlType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@TypeDefs(@TypeDef(name = Saksopplysning.XMLTYPE, typeClass = HibernateXmlType.class))
@Entity
@Table(name = "saksopplysning_dokument_kilde")
public class SaksopplysningDokumentKilde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksopplysning_id", nullable = false, updatable = false)
    public Saksopplysning saksopplysning;

    @Enumerated(EnumType.STRING)
    @Column(name = "kilde", nullable = false, updatable = false)
    private SaksopplysningKilde kilde;

    @Type(type = Saksopplysning.XMLTYPE)
    @Column(name = "dokument_xml", nullable = false)
    public String dokumentXml;

    public SaksopplysningDokumentKilde() {}

    public SaksopplysningDokumentKilde(Saksopplysning saksopplysning, SaksopplysningKilde kilde, String dokumentXml) {
        this.saksopplysning = saksopplysning;
        this.kilde = kilde;
        this.dokumentXml = dokumentXml;
    }
}
