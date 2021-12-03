package no.nav.melosys.service.persondata.mapping;

import java.util.Collection;
import java.util.Comparator;

import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.Folkeregisterpersonstatus;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FolkeregisterpersonstatusOversetter {
    private static final Logger log = LoggerFactory.getLogger(FolkeregisterpersonstatusOversetter.class);

    private FolkeregisterpersonstatusOversetter() {
    }

    public static Folkeregisterpersonstatus oversett(
        Collection<no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisterpersonstatus> folkeregisterpersonstatus) {
        return folkeregisterpersonstatus.stream().max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(FolkeregisterpersonstatusOversetter::oversett)
            .orElse(null);
    }

    private static Folkeregisterpersonstatus oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisterpersonstatus folkeregisterpersonstatus) {
        return new Folkeregisterpersonstatus(oversettStatusTilKodeverk(folkeregisterpersonstatus),
            folkeregisterpersonstatus.status());
    }

    private static Personstatuser oversettStatusTilKodeverk(
        no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisterpersonstatus folkeregisterpersonstatus) {
        return switch (folkeregisterpersonstatus.status()) {
            case "bosatt" -> Personstatuser.BOSATT;
            case "utflyttet" -> Personstatuser.UTFLYTTET;
            case "forsvunnet" -> Personstatuser.FORSVUNNET;
            case "doed" -> Personstatuser.DOED;
            case "opphoert" -> Personstatuser.OPPHOERT;
            case "foedselsregistrert" -> Personstatuser.FOEDSELSREGISTRERT;
            case "ikkeBosatt" -> Personstatuser.IKKE_BOSATT;
            case "midlertidig" -> Personstatuser.MIDLERTIDIG;
            case "inaktiv" -> Personstatuser.INAKTIV;
            default -> loggUkjentStatus(folkeregisterpersonstatus.status());
        };
    }

    private static Personstatuser loggUkjentStatus(String pdlStatus) {
        log.warn("Fikk ukjent personstatus fra PDL: {}", pdlStatus);
        return Personstatuser.UDEFINERT;
    }
}
