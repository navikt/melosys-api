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
import no.nav.melosys.exception.TekniskException;

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

    @ManyToOne(optional = true)
    @JoinColumn(name = "behandling_id", nullable = true, updatable = true)
    private Behandling behandling;

    @Column(name = "data", nullable = true, updatable = true)
    @Convert(converter = PropertiesConverter.class)
    private Properties data = new Properties();

    @Column(name = "steg", nullable = false, updatable = true)
    @Convert(converter = ProsessSteg.DbKonverterer.class)
    private ProsessSteg steg;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "antall_retry", nullable = false, updatable = true)
    private int antallRetry;
   
    @Column(name = "sist_forsoekt", nullable = true, updatable = true)
    private LocalDateTime sistForsøkt;

    @Column(name = "sover_til", nullable = true, updatable = true)
    private Instant soverTil;
    
    @Column(name = "endret_dato", nullable = false, updatable = true)
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
     * @throws TekniskException hvis feil ved deserialisering
     */
    public <T> T getData(ProsessDataKey key, Class<T> type) throws TekniskException {
        String dataString = getData(key);
        if (dataString == null) {
            return null;
        }
        try {
            return dataMapper.readValue(dataString, type);
        } catch (IOException e) {
            throw new TekniskException("Feil ved deserialiserigng", e);
        }
    }

    public void setData(ProsessDataKey key, String value) {
        this.data.setProperty(key.getKode(), value);
    }

    /**
     * Setter et dataelement til et objet (ved json serialisering)
     * @throws TekniskException hvis feil ved deserialisering
     */
    public void setData(ProsessDataKey key, Object value) throws TekniskException {
        try {
            String dataString = dataMapper.writeValueAsString(value);
            setData(key, dataString);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Feil ved serialiserigng", e);
        }
    }

    public void addData(Properties data) {
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

    public LocalDateTime getSistForsøkt() {
        return sistForsøkt;
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

    public void setHendelser(List<ProsessinstansHendelse> hendelser) {
        this.hendelser = hendelser;
    }
    
    public void leggTilHendelse(ProsessinstansHendelse piHend) {
        if (!this.equals(piHend.getProsessinstans())) {
            throw new TekniskException("Forsøk på å legge til ProsessinstansHendelse på feil Prosessinstans");
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
            throw new TekniskException("Prosessinstans.equals ble kalt før prosessinstans har fått saksnummer");
        }
        return this.id == that.id;
    }

}
