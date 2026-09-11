package com.omraty.backend.scheduler;

import com.omraty.backend.repository.VipRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Passe en CANCELLED toute demande VIP dont l'offre (OFFER_SENT) a expiré sans réponse du client.
 * Le client doit alors soumettre une nouvelle demande depuis le début s'il est toujours intéressé.
 * Contrairement au nettoyage quotidien des refresh tokens (RefreshTokenCleanupTask), tourne toutes
 * les 5 minutes : une offre de 24h doit être vue comme expirée rapidement, pas jusqu'à 24h plus
 * tard.
 */
@Component
public class VipRequestExpirationTask {

    private static final Logger log = LoggerFactory.getLogger(VipRequestExpirationTask.class);

    private final VipRequestRepository vipRequestRepository;

    public VipRequestExpirationTask(VipRequestRepository vipRequestRepository) {
        this.vipRequestRepository = vipRequestRepository;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void expireUnansweredOffers() {
        int expiredCount = vipRequestRepository.expireOffers();
        if (expiredCount > 0) {
            log.info("{} offre(s) VIP expirée(s) passée(s) à CANCELLED", expiredCount);
        }
    }
}
