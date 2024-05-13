package no.nav.melosys.saksflytapi.domain;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.jpa.PropertiesConverter;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.annotations.GenericGenerator;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Arbeidstabell for saksflyt.
 */
@Entity
@Table(name = "prosessinstans")
public class Prosessinstans {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "prosess_type", nullable = false)
    private ProsessType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProsessStatus status;

    @ManyToOne()
    @JoinColumn(name = "behandling_id")
    private Behandling behandling;

    @Lob
    @Column(name = "data")
    @Convert(converter = PropertiesConverter.class)
    private final Properties data = new Properties();

    @Enumerated(EnumType.STRING)
    @Column(name = "sist_fullfort_steg")
    private ProsessSteg sistFullførtSteg;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "endret_dato", nullable = false)
    private LocalDateTime endretDato;

    @OneToMany(mappedBy = "prosessinstans", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ProsessinstansHendelse> hendelser = new ArrayList<>();

    @Column(name = "sed_laas_referanse")
    private String låsReferanse;

    private static final ObjectMapper dataMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule().addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()));

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProsessType getType() {
        return type;
    }

    public void setType(ProsessType type) {
        this.type = type;
    }

    public ProsessStatus getStatus() {
        return status;
    }

    public void setStatus(ProsessStatus status) {
        this.status = status;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public Properties getData() {
        return data;
    }

    public boolean hasData(ProsessDataKey key) {
        return !isEmpty(getData(key));
    }

    /**
     * Returnerer et dataelement som String
     */
    public String getData(ProsessDataKey key) {
        return data.getProperty(key.getKode());
    }

    /**
     * Returnerer et dataelement som et Object (etter JSON deserialisering)
     */
    public <T> T getData(ProsessDataKey key, Class<T> type) {
        String dataString = getData(key);
        if (dataString == null) {
            return null;
        }
        try {
            return dataMapper.readValue(dataString, type);
        } catch (IOException e) {
            if (e instanceof JsonParseException) {
                throw new IllegalStateException("Feil ved deserialisering");
            } else {
                throw new IllegalStateException("Feil ved deserialisering", e);
            }
        }
    }

    public <T> T getData(ProsessDataKey key, Class<T> type, T defaultVerdi) {
        return Optional.ofNullable(getData(key, type)).orElse(defaultVerdi);
    }

    public <T> T getData(ProsessDataKey key, TypeReference<T> type) {
        String dataString = getData(key);
        if (dataString == null) {
            return null;
        }
        try {
            return dataMapper.readValue(dataString, type);
        } catch (JsonProcessingException e) {
            if (e instanceof JsonParseException) {
                throw new IllegalStateException("Feil ved deserialisering");
            } else {
                throw new IllegalStateException("Feil ved deserialisering", e);
            }
        }
    }

    public <T> T getData(ProsessDataKey key, TypeReference<T> type, T defaultVerdi) {
        return Optional.ofNullable(getData(key, type)).orElse(defaultVerdi);
    }

    public void setDataHvisIkkeTom(ProsessDataKey key, String value) {
        if (StringUtils.isNotEmpty(value)) {
            this.data.setProperty(key.getKode(), value);
        }
    }
    public void setData(ProsessDataKey key, String value) {
        if (value != null) {
            this.data.setProperty(key.getKode(), value);
        }
    }

    /**
     * Setter et dataelement til et object (ved json serialisering)
     */
    public void setData(ProsessDataKey key, Object value) {
        try {
            String dataString = dataMapper.writeValueAsString(value);
            setData(key, dataString);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Feil ved serialisering", e);
        }
    }

    public void setData(Properties data) {
        this.data.putAll(data);
    }

    public ProsessSteg getSistFullførtSteg() {
        return sistFullførtSteg;
    }

    public void setSistFullførtSteg(ProsessSteg sistFullførteSteg) {
        this.sistFullførtSteg = sistFullførteSteg;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public List<ProsessinstansHendelse> getHendelser() {
        return hendelser;
    }

    public String getLåsReferanse() {
        return låsReferanse;
    }

    public void setLåsReferanse(String låsReferanse) {
        this.låsReferanse = låsReferanse;
    }

    public String hentJournalpostID() {
        return Optional.ofNullable(getData(ProsessDataKey.JOURNALPOST_ID))
            .orElse(behandling.getInitierendeJournalpostId());
    }

    public String hentSaksbehandlerHvisTilordnes() {
        return Optional.ofNullable(getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class))
            .orElse(Boolean.FALSE) ? getData(ProsessDataKey.SAKSBEHANDLER) : null;
    }

    public String hentAktørIDFraDataEllerSED() {
        return Optional.ofNullable(getData(ProsessDataKey.AKTØR_ID))
            .orElse(getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class, new MelosysEessiMelding()).getAktoerId());
    }

    public MelosysEessiMelding hentMelosysEessiMelding() {
        return getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
    }

    public void leggTilHendelse(ProsessSteg steg, Throwable t) {
        this.hendelser.add(
            new ProsessinstansHendelse(
                this,
                LocalDateTime.now(),
                steg,
                t.getClass().getSimpleName(),
                ExceptionUtils.getStackTrace(t)
            )
        );
    }

    public boolean erFerdig() {
        return status == ProsessStatus.FERDIG;
    }

    public boolean erFeilet() {
        return status == ProsessStatus.FEILET;
    }

    public boolean erPåVent() {
        return status == ProsessStatus.PÅ_VENT;
    }

    public boolean erUnderBehandling() {
        return status == ProsessStatus.UNDER_BEHANDLING;
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Prosessinstans that)) {
            return false;
        }
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public String toString() {
        return "Prosessinstans{" + "id=" + id + ", type=" + type + ", status=" + status + ", behandling=" + behandling
            + ", sistFullførtSteg=" + sistFullførtSteg + ", registrertDato=" + registrertDato
            + ", endretDato=" + endretDato + ", hendelser=" + hendelser + ", låsReferanse='" + låsReferanse + '\'' + '}';
    }
}
