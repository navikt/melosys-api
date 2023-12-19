package no.nav.melosys.domain.mottatteopplysninger;

import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.jpa.MottatteOpplysningerListener;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;

@Entity
@EntityListeners(MottatteOpplysningerListener.class)
@Table(name = "mottatteopplysninger")
public class MottatteOpplysninger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Behandling behandling;

    @Column(name = "versjon", nullable = false)
    private String versjon;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false)
    private Instant endretDato;

    /**
     * @deprecated Mottaksdato har blitt flyttet til behandlingsårsak
     */
    @Deprecated()
    @Column(name = "mottaksdato")
    private LocalDate mottaksdato;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Mottatteopplysningertyper type;

    @Lob
    @Column(name = "original_data", updatable = false)
    private String originalData;

    @Lob
    @Column(name = "data", nullable = false)
    private String jsonData;

    @Column(name = "ekstern_referanse_id")
    private String eksternReferanseID;

    @Transient
    private MottatteOpplysningerData mottatteOpplysningerData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersjon() {
        return versjon;
    }

    public void setVersjon(String versjon) {
        this.versjon = versjon;
    }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    /**
     * @deprecated Mottaksdato har blitt flyttet til behandlingsårsak
     */
    @Deprecated()
    public LocalDate getMottaksdato() {
        return mottaksdato;
    }

    /**
     * @deprecated Mottaksdato har blitt flyttet til behandlingsårsak
     */
    @Deprecated()
    public void setMottaksdato(LocalDate mottaksdato) {
        this.mottaksdato = mottaksdato;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public String getOriginalData() {
        return originalData;
    }

    public void setOriginalData(String originalData) {
        this.originalData = originalData;
    }

    public Mottatteopplysningertyper getType() {
        return type;
    }

    public void setType(Mottatteopplysningertyper type) {
        this.type = type;
    }

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public String getEksternReferanseID() {
        return eksternReferanseID;
    }

    public void setEksternReferanseID(String eksternReferanseID) {
        this.eksternReferanseID = eksternReferanseID;
    }

    public MottatteOpplysningerData getMottatteOpplysningerData() {
        return mottatteOpplysningerData;
    }

    public void setMottatteOpplysningerData(MottatteOpplysningerData mottatteOpplysningerData) {
        this.mottatteOpplysningerData = mottatteOpplysningerData;
    }
}
