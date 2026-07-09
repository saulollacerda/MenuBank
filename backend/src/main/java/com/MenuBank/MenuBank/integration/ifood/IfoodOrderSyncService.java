package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.integration.ifood.dto.IfoodEventResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodOrderDetailResponse;
import com.MenuBank.MenuBank.integration.ifood.services.IfoodOrderImportService;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Orquestra o polling de eventos do iFood: busca eventos de todos os merchants
 * autorizados de uma vez (token é app-level), processa os eventos do ciclo de vida
 * (CONFIRMED importa cedo como PENDING, CONCLUDED solidifica para PAID, CANCELLED
 * cancela e tira dos ganhos) e reconhece todos os eventos recebidos — inclusive os
 * ignorados — para não acumular backlog na fila do iFood.
 *
 * <p>O {@code fullCode} é aceito com e sem o prefixo {@code ORDER_} (case-insensitive):
 * o polling real retorna eventos enxutos sem prefixo, mas os payloads de referência do
 * estilo webhook usam a forma prefixada.
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

    private static final String CONFIRMED = "CONFIRMED";
    private static final String CANCELLED = "CANCELLED";
    private static final String CONCLUDED = "CONCLUDED";
    private static final String ORDER_PREFIX = "ORDER_";

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
        List<String> ifoodMerchantIds = merchantRepository
                .findAllByIfoodMerchantIdIsNotNullAndIfoodOrderSyncEnabledTrue()
                .stream()
                .map(Merchant::getIfoodMerchantId)
                .toList();
        if (ifoodMerchantIds.isEmpty()) {
            log.info("[iFood] polling ignorado — nenhum merchant autorizado com sincronia ativa");
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
            try {
                processEvent(token, event);
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("[iFood] detalhe do pedido {} indisponível (404) — pulando", event.getOrderId());
            } catch (RuntimeException e) {
                log.error("[iFood] ERRO ao processar evento {} do pedido {}: {}",
                        event.getFullCode(), event.getOrderId(), e.getMessage(), e);
            }
        }

        List<String> eventIds = events.stream().map(IfoodEventResponse::getId).toList();
        withRetryOn401(token, t -> {
            orderClient.acknowledgeEvents(t, eventIds);
            return null;
        });
    }

    private void processEvent(String token, IfoodEventResponse event) {
        switch (normalizeFullCode(event.getFullCode())) {
            case CONFIRMED -> importOrder(token, event, OrderStatus.PENDING);
            case CONCLUDED -> {
                if (!importService.concludeOrder(event.getOrderId(), event.getMerchantId())) {
                    importOrder(token, event, OrderStatus.PAID);
                }
            }
            case CANCELLED -> {
                if (!importService.cancelOrder(event.getOrderId(), event.getMerchantId())) {
                    importOrder(token, event, OrderStatus.CANCELLED);
                }
            }
            default -> { /* apenas reconhecido, sem ação de negócio */ }
        }
    }

    private void importOrder(String token, IfoodEventResponse event, OrderStatus status) {
        IfoodOrderDetailResponse detail =
                withRetryOn401(token, t -> orderClient.getOrderDetail(t, event.getOrderId()));
        importService.importOrder(detail, status);
    }

    private static String normalizeFullCode(String fullCode) {
        if (fullCode == null) {
            return "";
        }
        String normalized = fullCode.toUpperCase(Locale.ROOT);
        return normalized.startsWith(ORDER_PREFIX) ? normalized.substring(ORDER_PREFIX.length()) : normalized;
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
