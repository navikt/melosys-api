package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.kodeverk.Kode;
import org.springframework.retry.annotation.Retryable;

@Retryable
public interface KodeOppslag {
    String getTermFraKodeverk(FellesKodeverk kodeverk, String kode);
    String getTermFraKodeverk(FellesKodeverk kodeverk, String kode, LocalDate dato);
    String getTermFraKodeverk(FellesKodeverk kodeverk, String kode, LocalDate dato, List<Kode> kodeperioder);
}
