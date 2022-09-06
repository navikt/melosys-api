package no.nav.melosys.service.soknad;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class SoknadMottatt {
    private String soknadID;
    private ZonedDateTime occuredOn;

    public SoknadMottatt() {
    }

    public SoknadMottatt(String soknadID, ZonedDateTime occuredOn) {
        this.soknadID = soknadID;
        this.occuredOn = occuredOn;
    }

    public String getSoknadID() {
        return soknadID;
    }

    public ZonedDateTime getOccuredOn() {
        return occuredOn;
    }

    @Override
    public String toString() {
        return "SoknadMottatt{" +
            "soknadID='" + soknadID + '\'' +
            ", occuredOn=" + occuredOn +
            '}';
    }

    public boolean erForGammelTilForvaltningsmelding() {
        return ChronoUnit.DAYS.between(occuredOn, ZonedDateTime.now()) >= 7;
    }
}
