package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyProductCategoryBackfill")
class LegacyProductCategoryBackfillTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private LegacyProductCategoryBackfill backfill;

    @Test
    @DisplayName("não faz nada quando não há produtos sem categoria")
    void shouldDoNothingWhenNoOrphans() throws Exception {
        given(productRepository.findAllByCategoryIsNull()).willReturn(List.of());

        backfill.run();

        then(categoryRepository).should(never()).save(any());
        then(productRepository).should(never()).saveAll(anyIterable());
    }

    @Test
    @DisplayName("cria a categoria 'Sem categoria' por owner e atribui aos produtos órfãos")
    void shouldCreateDefaultCategoryAndAssignToOrphans() throws Exception {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();

        Product p1 = Product.builder().id(UUID.randomUUID()).ownerId(ownerA).name("Produto 1").build();
        Product p2 = Product.builder().id(UUID.randomUUID()).ownerId(ownerA).name("Produto 2").build();
        Product p3 = Product.builder().id(UUID.randomUUID()).ownerId(ownerB).name("Produto 3").build();

        given(productRepository.findAllByCategoryIsNull()).willReturn(List.of(p1, p2, p3));
        given(categoryRepository.findByNameAndOwnerId("Sem categoria", ownerA)).willReturn(Optional.empty());
        given(categoryRepository.findByNameAndOwnerId("Sem categoria", ownerB)).willReturn(Optional.empty());

        Category createdA = Category.builder().id(UUID.randomUUID()).ownerId(ownerA).name("Sem categoria").build();
        Category createdB = Category.builder().id(UUID.randomUUID()).ownerId(ownerB).name("Sem categoria").build();
        given(categoryRepository.save(argThat(c -> c != null && ownerA.equals(c.getOwnerId()))))
                .willReturn(createdA);
        given(categoryRepository.save(argThat(c -> c != null && ownerB.equals(c.getOwnerId()))))
                .willReturn(createdB);

        backfill.run();

        then(categoryRepository).should(times(2)).save(any(Category.class));

        ArgumentCaptor<Iterable<Product>> captor = ArgumentCaptor.captor();
        then(productRepository).should(times(2)).saveAll(captor.capture());

        List<Iterable<Product>> savedBatches = captor.getAllValues();
        assertThat(savedBatches).hasSize(2);

        boolean hasOwnerABatch = savedBatches.stream().anyMatch(batch -> {
            List<Product> list = toList(batch);
            return list.size() == 2 && list.stream().allMatch(p -> p.getCategory() == createdA);
        });
        boolean hasOwnerBBatch = savedBatches.stream().anyMatch(batch -> {
            List<Product> list = toList(batch);
            return list.size() == 1 && list.get(0).getCategory() == createdB;
        });
        assertThat(hasOwnerABatch).as("owner A batch with 2 products tagged createdA").isTrue();
        assertThat(hasOwnerBBatch).as("owner B batch with 1 product tagged createdB").isTrue();
    }

    @Test
    @DisplayName("reutiliza categoria 'Sem categoria' existente em vez de duplicar (idempotência)")
    void shouldReuseExistingDefaultCategory() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Product orphan = Product.builder().id(UUID.randomUUID()).ownerId(ownerId).name("Produto").build();
        Category existing = Category.builder().id(UUID.randomUUID()).ownerId(ownerId).name("Sem categoria").build();

        given(productRepository.findAllByCategoryIsNull()).willReturn(List.of(orphan));
        given(categoryRepository.findByNameAndOwnerId("Sem categoria", ownerId)).willReturn(Optional.of(existing));

        backfill.run();

        then(categoryRepository).should(never()).save(any(Category.class));
        then(productRepository).should().saveAll(argThat(iter -> {
            List<Product> list = toList(iter);
            return list.size() == 1 && list.get(0).getCategory() == existing;
        }));
    }

    private static <T> List<T> toList(Iterable<T> iter) {
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        iter.forEach(list::add);
        return list;
    }
}
