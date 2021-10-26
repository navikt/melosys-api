package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.storbritannia.*;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class AttestStorbritannia extends DokgenDto {

    private final Arbeidstaker arbeidstaker;
    private final MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
    private final ArbeidsgiverNorge arbeidsgiverNorge;
    private final Utsendelse utsendelse;
    private final RepresentantUK representantUK;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant vedtaksdato;


    public AttestStorbritannia(AttestStorbritanniaBrevbestilling brevbestilling) {
        super(brevbestilling);
        this.arbeidstaker = Arbeidstaker.av(brevbestilling.getArbeidstaker());
        this.medfolgendeFamiliemedlemmer = MedfolgendeFamiliemedlemmer.av(brevbestilling.getMedfolgendeFamiliemedlemmer());
        this.arbeidsgiverNorge = ArbeidsgiverNorge.av(brevbestilling.getArbeidsgiverNorge());
        this.utsendelse = Utsendelse.av(brevbestilling.getUtsendelse());
        this.representantUK = RepresentantUK.av(brevbestilling.getRepresentantUK());
        this.vedtaksdato = brevbestilling.getVedtaksdato();
    }

    public static AttestStorbritannia av(AttestStorbritanniaBrevbestilling brevbestilling) {
        return new AttestStorbritannia(brevbestilling);
    }
}
