package no.nav.melosys.domain.mottatteopplysninger;

import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.jpa.MottatteOpplysningerListener;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;

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

    @Column(name = "mottaksdato")
    private LocalDate mottaksdato;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Behandlingsgrunnlagtyper type;

    @Lob
    @Column(name = "original_data", updatable = false)
    private String originalData;

    @Lob
    @Column(name = "data")
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

    public LocalDate getMottaksdato() {
        return mottaksdato;
    }

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

    public Behandlingsgrunnlagtyper getType() {
        return type;
    }

    public void setType(Behandlingsgrunnlagtyper type) {
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

    public void setMottatteOpplysningerdata(MottatteOpplysningerData mottatteOpplysningerData) {
        this.mottatteOpplysningerData = mottatteOpplysningerData;
    }

    public boolean erSøknad() {
        return erSøknadOmA1() || erSøknadFtrl();
    }

    public boolean erSøknadOmA1() {
        return type == Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS
            || type == Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS;
    }

    public boolean erSøknadFtrl() {
        return type == Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN;
    }

    public boolean erSed() {
        return this.type == Behandlingsgrunnlagtyper.SED;
    }
}
