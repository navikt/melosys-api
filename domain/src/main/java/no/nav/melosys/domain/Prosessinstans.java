package no.nav.melosys.domain;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.jpa.PropertiesConverter;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Arbeidstabell for saksflyt.
 */
@Entity
@Table(name = "prosessinstans")
public class Prosessinstans {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "prosess_type", nullable = false, updatable = false)
    @Convert(converter = ProsessType.DbKonverterer.class)
    private ProsessType type;

    @ManyToOne()
    @JoinColumn(name = "behandling_id")
    private Behandling behandling;

    @Column(name = "data")
    @Convert(converter = PropertiesConverter.class)
    private Properties data = new Properties();

    @Column(name = "steg", nullable = false)
    @Convert(converter = ProsessSteg.DbKonverterer.class)
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
    
    private static ObjectMapper dataMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public long getId() {
        return id;
    }

    public ProsessType getType() {
        return type;
    }

    public void setType(ProsessType type) {
        this.type = type;
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
            throw new RuntimeException("Feil ved deserialisering", e);
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
            throw new RuntimeException("Feil ved serialisering", e);
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

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

    public List<ProsessinstansHendelse> getHendelser() {
        return hendelser;
    }

    private static final int VARCHAR2_MAX_BYTES = 4000;
    
    private void leggTilHendelse(ProsessinstansHendelse piHend) {
        if (!this.equals(piHend.getProsessinstans())) {
            // Holder med RTE, siden det skal mye til for at en slik feil kommer ut i prod
            throw new RuntimeException("Forsøk på å legge til ProsessinstansHendelse på feil Prosessinstans");
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
            String loggMelding = melding + " - " + ExceptionUtils.getStackTrace(t);
            if (loggMelding.getBytes().length > VARCHAR2_MAX_BYTES) {
                loggMelding = new String(loggMelding.getBytes(), 0, VARCHAR2_MAX_BYTES);
            }
            leggTilHendelse(type, loggMelding);
        } else {
            leggTilHendelse(type, melding);
        }
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Prosessinstans)) { // Implisitt nullsjekk
            return false;
        }
        Prosessinstans that = (Prosessinstans) o;
        if (this.id == 0) {
            // Holder med RTE, siden det skal mye til for at en slik feil kommer ut i prod
            throw new RuntimeException("Prosessinstans.equals ble kalt før prosessinstans har fått saksnummer");
        }
        return this.id == that.id;
    }

}
