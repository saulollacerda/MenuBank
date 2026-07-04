package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.integration.ifood.dto.IfoodEventResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodOrderDetailResponse;
import com.MenuBank.MenuBank.integration.ifood.services.IfoodOrderImportService;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.function.Function;

/**
 * Orquestra o polling de eventos do iFood: busca eventos de todos os merchants
 * autorizados de uma vez (token é app-level), importa os pedidos CONCLUDED e
 * reconhece todos os eventos recebidos — inclusive os ignorados — para não
 * acumular backlog na fila do iFood.
 *
 * <p>Padrão de retry em 401: um {@code accessToken} pode expirar entre o
 * {@code getAccessToken()} e a chamada; nesse caso força-se o refresh via
 * {@link IfoodTokenService#handleUnauthorized()} e a chamada é repetida uma
 * única vez. Futuras integrações iFood (rastreamento, disputas) devem reusar
 * esse mesmo padrão.
 */
@Service
public class IfoodOrderSyncService {

    private static final Logger log = LoggerFactory.getLogger(IfoodOrderSyncService.class);

    private static final String CONCLUDED = "CONCLUDED";

    private final IfoodOrderClient orderClient;
    private final IfoodTokenService tokenService;
    private final MerchantRepository merchantRepository;
    private final IfoodOrderImportService importService;

    public IfoodOrderSyncService(IfoodOrderClient orderClient,
                                 IfoodTokenService tokenService,
                                 MerchantRepository merchantRepository,
                                 IfoodOrderImportService importService) {
        this.orderClient = orderClient;
        this.tokenService = tokenService;
        this.merchantRepository = merchantRepository;
        this.importService = importService;
    }

    public void syncOrders() {
        List<String> ifoodMerchantIds = merchantRepository.findAllByIfoodMerchantIdIsNotNull()
                .stream()
                .map(Merchant::getIfoodMerchantId)
                .toList();
        if (ifoodMerchantIds.isEmpty()) {
            log.info("[iFood] polling ignorado — nenhum merchant autorizado");
            return;
        }

        log.info("[iFood] polling iniciado — merchants={}", ifoodMerchantIds.size());

        String token = tokenService.getAccessToken();
        List<IfoodEventResponse> events =
                withRetryOn401(token, t -> orderClient.pollEvents(t, ifoodMerchantIds));
        if (events.isEmpty()) {
            log.info("[iFood] polling concluído — nenhum evento novo");
            return;
        }

        log.info("[iFood] polling retornou {} eventos", events.size());

        for (IfoodEventResponse event : events) {
            if (!CONCLUDED.equalsIgnoreCase(event.getFullCode())) {
                continue;
            }
            try {
                IfoodOrderDetailResponse detail =
                        withRetryOn401(token, t -> orderClient.getOrderDetail(t, event.getOrderId()));
                importService.importOrder(detail);
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("[iFood] detalhe do pedido {} indisponível (404) — pulando", event.getOrderId());
            } catch (RuntimeException e) {
                log.error("[iFood] ERRO ao importar pedido {}: {}", event.getOrderId(), e.getMessage(), e);
            }
        }

        List<String> eventIds = events.stream().map(IfoodEventResponse::getId).toList();
        withRetryOn401(token, t -> {
            orderClient.acknowledgeEvents(t, eventIds);
            return null;
        });
    }

    private <T> T withRetryOn401(String token, Function<String, T> call) {
        try {
            return call.apply(token);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("[iFood] 401 recebido — forçando refresh do token e repetindo a chamada");
            return call.apply(tokenService.handleUnauthorized());
        }
    }
}
