package no.nav.melosys.tjenester.gui.kontroll.regel;

import java.time.LocalDate;

import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping(value = "/kontroll/regel")
public class PeriodeReglerController {

    @GetMapping("/periodeover2årog1dag")
    public ResponseEntity<ResponseDto> harPeriodeOver2ÅrOgEnDag(@RequestBody RequestDto requestDto) {
        boolean harFeil = PeriodeRegler.periodeOver2ÅrOgEnDag(requestDto.periodeFom(), requestDto.periodeTom());
        return ResponseEntity.ok(ResponseDto.of(harFeil));
    }

    record RequestDto(LocalDate periodeFom, LocalDate periodeTom) {
    }

    record ResponseDto(boolean harFeil) {
        static ResponseDto of(boolean harFeil) {
            return new ResponseDto(harFeil);
        }
    }
}
