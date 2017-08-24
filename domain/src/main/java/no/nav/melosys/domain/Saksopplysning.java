package no.nav.melosys.domain;

import static no.nav.melosys.domain.SaksopplysningKilde.TPS;
import static no.nav.melosys.domain.SaksopplysningType.PERSONOPPLYSNING;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
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
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.hibernate.annotations.ColumnTransformer;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
@Entity
@Table(name = "SAKSOPPLYSNING")
public class Saksopplysning implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "opplysning_type", nullable = false, updatable = false)
    @Convert(converter = SaksopplysningType.DbKonverterer.class)
    private SaksopplysningType type;

    @Column(name="versjon", nullable = false, updatable = false)
    private int versjon;

    @Column(name = "kilde", nullable = false, updatable = false)
    @Convert(converter = SaksopplysningKilde.DbKonverterer.class)
    private SaksopplysningKilde kilde;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "dokument_xml", updatable = false)
    @ColumnTransformer(read = "to_clob(dokument_xml)", write = "?")
    private String dokumentXml;
    
    @Transient
    // TODO (farjam 2017-08-24). Dette skal representeres med intern modell. Se EESSI2-223
    private Object dokument;

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

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
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

    public Object getDokument() {
        return dokument;
    }

    public void setDokument(Object dokument) {
        this.dokument = dokument;
    }

    @PostLoad
    private void lagDokument() {
        try {
            if (dokumentXml == null) {
                dokument = null;
                return;
            } else if (type == PERSONOPPLYSNING && kilde == TPS) {
                StringReader reader = new StringReader(dokumentXml);
                JAXBContext jaxbContext = JAXBContext.newInstance(HentPersonResponse.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                dokument = unmarshaller.unmarshal(reader);
            } else {
                // FIXME: Legg til relevante typer
                throw new RuntimeException(String.format("Ukjent dokument: type=%1s, kilde=%2s, versjon=%3", type, kilde, versjon));
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Feil i dokumentXml", e);
        }
    }
    
    @PrePersist
    @PreUpdate
    private void lagDokumentXml() {
        try {
            if (dokument == null) {
                dokumentXml = null;
            } else if (type == PERSONOPPLYSNING && kilde == TPS) {
                HentPersonResponse hentPersonResponse = (HentPersonResponse) dokument;
                JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse.class);
                no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse xmlRoot = new no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse();
                xmlRoot.setResponse(hentPersonResponse);
                StringWriter writer = new StringWriter();
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.marshal(xmlRoot, writer);
                dokumentXml = writer.toString();
            } else {
                // FIXME: Legg til relevante typer
                throw new RuntimeException(String.format("Ukjent dokument: type=%1s, kilde=%2s, versjon=%3", type, kilde, versjon));
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Feil i dokumentXml", e);
        }
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
        return Objects.equals(this.dokumentXml, that.dokumentXml)
            && Objects.equals(this.registrertDato, that.registrertDato)
            && Objects.equals(this.behandling, that.behandling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentXml, registrertDato, behandling);
    }

}
