package no.nav.melosys.service.journalforing;

import no.nav.melosys.service.AdminTjeneste;
import no.nav.melosys.service.eessi.jobb.JournalfoerX100JournalposterJobb;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping("/admin/journalposter")
public class JournalfoeringAdminTjeneste implements AdminTjeneste {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringAdminTjeneste.class);

    private final String apiKey;
    private final JournalfoerX100JournalposterJobb journalfoerX100JournalposterJobb;

    public JournalfoeringAdminTjeneste(@Value("${Melosys-admin.apikey}") String apiKey,
                                       JournalfoerX100JournalposterJobb journalfoerX100JournalposterJobb) {
        this.apiKey = apiKey;
        this.journalfoerX100JournalposterJobb = journalfoerX100JournalposterJobb;
    }

    @PutMapping("/x100/ferdigstilling")
    public ResponseEntity<Void> journalfoerX100Journalposter(@RequestHeader(API_KEY_HEADER) String apiKey) {
        validerApikey(apiKey);
        log.info("Forsøker endelig journalføring av journalposter opprettet for X100 SED-er.");

        journalfoerX100JournalposterJobb.journalfoerX100Journalposter();

        return ResponseEntity.noContent().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
