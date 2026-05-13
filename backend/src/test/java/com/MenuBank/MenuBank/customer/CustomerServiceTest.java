package com.MenuBank.MenuBank.customer;

import com.MenuBank.MenuBank.common.UserContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private CustomerService customerService;

    private UUID ownerId;
    private UUID customerId;
    private Customer customer;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customerRequest = CustomerRequest.builder()
                .name("João Silva")
                .phone("11999999999")
                .email("joao@email.com")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .ownerId(ownerId)
                .name("João Silva")
                .phone("11999999999")
                .email("joao@email.com")
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar cliente com dados válidos e retornar CustomerResponse")
        void shouldCreateCustomerAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.save(any(Customer.class))).willReturn(customer);

            CustomerResponse result = customerService.create(customerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId);
            assertThat(result.getName()).isEqualTo(customerRequest.getName());
            assertThat(result.getPhone()).isEqualTo(customerRequest.getPhone());
            assertThat(result.getEmail()).isEqualTo(customerRequest.getEmail());
            then(customerRepository).should().save(argThat(c -> ownerId.equals(c.getOwnerId())));
        }

        @Test
        @DisplayName("deve criar cliente sem telefone e email (campos opcionais)")
        void shouldCreateCustomerWithoutOptionalFields() {
            CustomerRequest minimalRequest = CustomerRequest.builder()
                    .name("Maria Souza")
                    .build();

            Customer minimalCustomer = Customer.builder()
                    .id(customerId)
                    .ownerId(ownerId)
                    .name("Maria Souza")
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.save(any(Customer.class))).willReturn(minimalCustomer);

            CustomerResponse result = customerService.create(minimalRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Maria Souza");
            assertThat(result.getPhone()).isNull();
            assertThat(result.getEmail()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar CustomerResponse quando cliente existe")
        void shouldReturnResponseWhenExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));

            CustomerResponse result = customerService.findById(customerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId);
            assertThat(result.getName()).isEqualTo(customer.getName());
            assertThat(result.getPhone()).isEqualTo(customer.getPhone());
            assertThat(result.getEmail()).isEqualTo(customer.getEmail());
        }

        @Test
        @DisplayName("deve lançar CustomerNotFoundException quando cliente não existe")
        void shouldThrowWhenCustomerNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findById(customerId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista de todos os clientes")
        void shouldReturnListOfAllCustomers() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findAllByOwnerId(ownerId)).willReturn(List.of(customer));

            List<CustomerResponse> result = customerService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(customerId);
            assertThat(result.get(0).getName()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há clientes")
        void shouldReturnEmptyList() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findAllByOwnerId(ownerId)).willReturn(List.of());

            List<CustomerResponse> result = customerService.findAll();

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
        @DisplayName("deve atualizar cliente existente e retornar CustomerResponse atualizado")
        void shouldUpdateAndReturnUpdatedResponse() {
            CustomerRequest updateRequest = CustomerRequest.builder()
                    .name("João Santos")
                    .phone("11988888888")
                    .email("joao.santos@email.com")
                    .build();

            Customer updatedCustomer = Customer.builder()
                    .id(customerId)
                    .ownerId(ownerId)
                    .name("João Santos")
                    .phone("11988888888")
                    .email("joao.santos@email.com")
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(customerRepository.save(any(Customer.class))).willReturn(updatedCustomer);

            CustomerResponse result = customerService.update(customerId, updateRequest);

            assertThat(result.getName()).isEqualTo("João Santos");
            assertThat(result.getPhone()).isEqualTo("11988888888");
            assertThat(result.getEmail()).isEqualTo("joao.santos@email.com");
        }

        @Test
        @DisplayName("deve lançar CustomerNotFoundException ao atualizar cliente inexistente")
        void shouldThrowWhenCustomerNotFoundForUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.update(customerId, customerRequest))
                    .isInstanceOf(CustomerNotFoundException.class);

            then(customerRepository).should(never()).save(any(Customer.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar cliente existente sem lançar exceção")
        void shouldDeleteExistingCustomer() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.existsByIdAndOwnerId(customerId, ownerId)).willReturn(true);
            willDoNothing().given(customerRepository).deleteByIdAndOwnerId(customerId, ownerId);

            assertThatNoException().isThrownBy(() -> customerService.delete(customerId));

            then(customerRepository).should().deleteByIdAndOwnerId(customerId, ownerId);
        }

        @Test
        @DisplayName("deve lançar CustomerNotFoundException ao deletar cliente inexistente")
        void shouldThrowWhenCustomerNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.existsByIdAndOwnerId(customerId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> customerService.delete(customerId))
                    .isInstanceOf(CustomerNotFoundException.class);

            then(customerRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}

