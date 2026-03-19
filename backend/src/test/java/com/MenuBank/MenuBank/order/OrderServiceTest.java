package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private Customer customer;
    private Product product;
    private Order order;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        customer = Customer.builder()
                .id(customerId)
                .name("Cliente Teste")
                .phone("11999999999")
                .email("cliente@email.com")
                .build();

        product = Product.builder()
                .id(productId)
                .name("Hambúrguer")
                .price(new BigDecimal("30.00"))
                .estimatedCost(new BigDecimal("12.00"))
                .status(ProductStatus.ACTIVE)
                .build();

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        orderRequest = OrderRequest.builder()
                .customerId(customerId)
                .items(List.of(itemRequest))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("30.00"))
                .build();

        order = Order.builder()
                .id(orderId)
                .dateTime(LocalDateTime.now())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalValue(new BigDecimal("60.00"))
                .estimatedProfit(new BigDecimal("36.00"))
                .items(List.of(orderItem))
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar pedido com dados válidos e retornar OrderResponse")
        void shouldCreateOrderAndReturnOrderResponse() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            OrderResponse result = orderService.create(orderRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getCustomerName()).isEqualTo("Cliente Teste");
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("deve definir status como PENDING por padrão")
        void shouldSetStatusToPendingByDefault() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getStatus() == OrderStatus.PENDING
            ));
        }

        @Test
        @DisplayName("deve calcular totalValue a partir dos itens (quantidade × preço)")
        void shouldCalculateTotalValueFromItems() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            // 2 × R$30.00 = R$60.00
            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getTotalValue().compareTo(new BigDecimal("60.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve calcular estimatedProfit (totalValue − custo total dos itens)")
        void shouldCalculateEstimatedProfitFromItems() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            // totalValue(60.00) − totalCost(2 × 12.00 = 24.00) = 36.00
            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getEstimatedProfit().compareTo(new BigDecimal("36.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve definir unitPrice do item com o preço atual do produto")
        void shouldSetItemUnitPriceFromProductPrice() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getItems().get(0).getUnitPrice()
                            .compareTo(new BigDecimal("30.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve definir dateTime no momento da criação")
        void shouldSetDateTimeOnCreation() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            LocalDateTime before = LocalDateTime.now();
            orderService.create(orderRequest);
            LocalDateTime after = LocalDateTime.now();

            then(orderRepository).should().save(argThat(savedOrder ->
                    !savedOrder.getDateTime().isBefore(before) &&
                    !savedOrder.getDateTime().isAfter(after)
            ));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando cliente não encontrado")
        void shouldThrowWhenCustomerNotFound() {
            given(customerRepository.findById(customerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(orderRequest))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Cliente");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando produto não encontrado")
        void shouldThrowWhenProductNotFound() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(orderRequest))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Produto");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve mapear corretamente os itens na resposta")
        void shouldMapItemsCorrectlyInResponse() {
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            OrderResponse result = orderService.create(orderRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductName()).isEqualTo("Hambúrguer");
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
            assertThat(result.getItems().get(0).getUnitPrice())
                    .isEqualByComparingTo(new BigDecimal("30.00"));
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar OrderResponse quando pedido existe")
        void shouldReturnOrderResponseWhenExists() {
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(orderId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getCustomerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista de todos os pedidos")
        void shouldReturnListOfAllOrders() {
            given(orderRepository.findAll()).willReturn(List.of(order));

            List<OrderResponse> result = orderService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há pedidos")
        void shouldReturnEmptyList() {
            given(orderRepository.findAll()).willReturn(List.of());

            List<OrderResponse> result = orderService.findAll();

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar pedido existente e retornar OrderResponse atualizado")
        void shouldUpdateAndReturnUpdatedOrderResponse() {
            UUID newProductId = UUID.randomUUID();
            Product newProduct = Product.builder()
                    .id(newProductId)
                    .name("Pizza")
                    .price(new BigDecimal("45.00"))
                    .estimatedCost(new BigDecimal("18.00"))
                    .status(ProductStatus.ACTIVE)
                    .build();

            OrderItemRequest newItemRequest = OrderItemRequest.builder()
                    .productId(newProductId)
                    .quantity(3)
                    .build();

            OrderRequest updateRequest = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(newItemRequest))
                    .build();

            OrderItem updatedItem = OrderItem.builder()
                    .id(UUID.randomUUID())
                    .product(newProduct)
                    .quantity(3)
                    .unitPrice(new BigDecimal("45.00"))
                    .build();

            Order updatedOrder = Order.builder()
                    .id(orderId)
                    .dateTime(order.getDateTime())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalValue(new BigDecimal("135.00"))
                    .estimatedProfit(new BigDecimal("81.00"))
                    .items(List.of(updatedItem))
                    .build();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(newProductId)).willReturn(Optional.of(newProduct));
            given(orderRepository.save(any(Order.class))).willReturn(updatedOrder);

            OrderResponse result = orderService.update(orderId, updateRequest);

            assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("135.00"));
            assertThat(result.getEstimatedProfit()).isEqualByComparingTo(new BigDecimal("81.00"));
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("deve recalcular totais ao atualizar pedido")
        void shouldRecalculateTotalsOnUpdate() {
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.update(orderId, orderRequest);

            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getTotalValue().compareTo(new BigDecimal("60.00")) == 0 &&
                    savedOrder.getEstimatedProfit().compareTo(new BigDecimal("36.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException ao atualizar pedido inexistente")
        void shouldThrowWhenOrderNotFoundForUpdate() {
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.update(orderId, orderRequest))
                    .isInstanceOf(OrderNotFoundException.class);

            then(orderRepository).should(never()).save(any(Order.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar pedido existente sem lançar exceção")
        void shouldDeleteExistingOrder() {
            given(orderRepository.existsById(orderId)).willReturn(true);
            willDoNothing().given(orderRepository).deleteById(orderId);

            assertThatNoException().isThrownBy(() -> orderService.delete(orderId));

            then(orderRepository).should().deleteById(orderId);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException ao deletar pedido inexistente")
        void shouldThrowWhenOrderNotFoundForDelete() {
            given(orderRepository.existsById(orderId)).willReturn(false);

            assertThatThrownBy(() -> orderService.delete(orderId))
                    .isInstanceOf(OrderNotFoundException.class);

            then(orderRepository).should(never()).deleteById(any());
        }
    }
}

