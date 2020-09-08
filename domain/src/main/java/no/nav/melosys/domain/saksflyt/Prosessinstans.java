package no.nav.melosys.domain.saksflyt;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import javax.persistence.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.jpa.PropertiesConverter;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.annotations.GenericGenerator;

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

    @Column(name = "data")
    @Convert(converter = PropertiesConverter.class)
    private final Properties data = new Properties();

    @Enumerated(EnumType.STRING)
    @Column(name = "steg", nullable = false)
    private ProsessSteg steg;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "antall_retry", nullable = false)
    private int antallRetry;

    @Column(name = "sist_forsoekt")
    private LocalDateTime sistForsøkt;

    @Column(name = "sover_til")
    private Instant soverTil;

    @Column(name = "endret_dato", nullable = false)
    private LocalDateTime endretDato;

    @OneToMany(mappedBy = "prosessinstans", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ProsessinstansHendelse> hendelser;

    private static final ObjectMapper dataMapper = new ObjectMapper().registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule().addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()));

    public UUID getId() {
        return id;
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

    /** Returnerer et dataelement som String */
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
            // Holder med RTE, siden det skal mye til for at en slik feil kommer ut i prod
            throw new IllegalStateException("Feil ved deserialisering", e);
        }
    }

    public <T> T getData(ProsessDataKey key, TypeReference<T> type) {
        String dataString = getData(key);
        if (dataString == null) {
            return null;
        }
        try {
            return dataMapper.readValue(dataString, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Feil ved deserialisering", e);
        }
    }

    public void setData(ProsessDataKey key, String value) {
        this.data.setProperty(key.getKode(), value);
    }

    /**
     * Setter et dataelement til et objet (ved json serialisering)
     */
    public void setData(ProsessDataKey key, Object value) {
        try {
            String dataString = dataMapper.writeValueAsString(value);
            setData(key, dataString);
        } catch (JsonProcessingException e) {
            // Holder med RTE, siden det skal mye til for at en slik feil kommer ut i prod
            throw new IllegalStateException("Feil ved serialisering", e);
        }
    }

    public void setData(Properties data) {
        this.data.putAll(data);
    }

    public ProsessSteg getSteg() {
        return steg;
    }

    public void setSteg(ProsessSteg steg) {
        this.steg = steg;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public int getAntallRetry() {
        return antallRetry;
    }

    public void setAntallRetry(int antallRetry) {
        this.antallRetry = antallRetry;
    }

    public void setSistForsøkt(LocalDateTime sistForsøkt) {
        this.sistForsøkt = sistForsøkt;
    }

    public Instant getSoverTil() {
        return soverTil;
    }

    public void setSoverTil(Instant soverTil) {
        this.soverTil = soverTil;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

    public List<ProsessinstansHendelse> getHendelser() {
        return hendelser;
    }

    public String hentJournalpostID() {
        return Optional.ofNullable(getData(ProsessDataKey.JOURNALPOST_ID))
            .orElse(behandling.getInitierendeJournalpostId());
    }

    public String hentSaksbehandlerHvisTilordnes() {
        return Optional.ofNullable(getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class))
            .orElse(Boolean.FALSE) ? getData(ProsessDataKey.SAKSBEHANDLER) : null;
    }

    private void leggTilHendelse(ProsessinstansHendelse piHend) {
        if (!this.equals(piHend.getProsessinstans())) {
            // Holder med RTE, siden det skal mye til for at en slik feil kommer ut i prod
            throw new IllegalArgumentException("Forsøk på å legge til ProsessinstansHendelse på feil Prosessinstans");
        }
        if (hendelser == null) {
            hendelser = new ArrayList<>();
        }
        hendelser.add(piHend);
    }

    public void leggTilHendelse(String type, String melding) {
        ProsessinstansHendelse pih = new ProsessinstansHendelse(this, LocalDateTime.now(), steg, type, melding);
        leggTilHendelse(pih);
    }

    public void leggTilHendelse(String type, String melding, Throwable t) {
        if (t != null) {
            leggTilHendelse(type, melding + " - " + ExceptionUtils.getStackTrace(t));
        } else {
            leggTilHendelse(type, melding);
        }
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
        if (!(o instanceof Prosessinstans)) {
            return false;
        }
        Prosessinstans that = (Prosessinstans) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public String toString() {
        return "Prosessinstans{" +
            "id=" + id +
            ", type=" + type +
            ", behandling=" + behandling +
            ", data=" + data +
            ", steg=" + steg +
            ", registrertDato=" + registrertDato +
            ", antallRetry=" + antallRetry +
            ", sistForsøkt=" + sistForsøkt +
            ", soverTil=" + soverTil +
            ", endretDato=" + endretDato +
            ", hendelser=" + hendelser +
            '}';
    }
}
