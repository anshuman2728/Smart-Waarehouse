package com.saadaoui.smartwarehouse.config;

import com.saadaoui.smartwarehouse.auth.entity.Role;
import com.saadaoui.smartwarehouse.auth.entity.User;
import com.saadaoui.smartwarehouse.auth.repository.RoleRepository;
import com.saadaoui.smartwarehouse.auth.repository.UserRepository;
import com.saadaoui.smartwarehouse.category.repository.CategoryRepository;
import com.saadaoui.smartwarehouse.entity.AppSettings;
import com.saadaoui.smartwarehouse.entity.Category;
import com.saadaoui.smartwarehouse.entity.Product;
import com.saadaoui.smartwarehouse.entity.Supplier;
import com.saadaoui.smartwarehouse.movement.service.MovementService;
import com.saadaoui.smartwarehouse.notification.repository.NotificationRepository;
import com.saadaoui.smartwarehouse.product.repository.ProductRepository;
import com.saadaoui.smartwarehouse.settings.repository.AppSettingsRepository;
import com.saadaoui.smartwarehouse.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock
    private Environment environment;

    @Mock
    private AppSettingsRepository settingsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MovementService movementService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demoDataSeeder, "managerEmail", "manager@smartwarehouse.com");
        ReflectionTestUtils.setField(demoDataSeeder, "managerPassword", "Manager@123");
        ReflectionTestUtils.setField(demoDataSeeder, "employeeEmail", "employee@smartwarehouse.com");
        ReflectionTestUtils.setField(demoDataSeeder, "employeePassword", "Employee@123");
    }

    @Test
    @DisplayName("Should do nothing when seedDemoData is disabled (false)")
    void shouldDoNothingWhenDisabled() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ReflectionTestUtils.setField(demoDataSeeder, "seedDemoData", false);

        demoDataSeeder.run(null);

        verifyNoInteractions(settingsRepository);
        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(productRepository);
        verifyNoInteractions(movementService);
    }

    @Test
    @DisplayName("Should strictly abort when active profile is prod")
    void shouldAbortWhenInProdProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ReflectionTestUtils.setField(demoDataSeeder, "seedDemoData", true);

        demoDataSeeder.run(null);

        verifyNoInteractions(settingsRepository);
        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(productRepository);
        verifyNoInteractions(movementService);
    }

    @Test
    @DisplayName("Should seed all entities and execute movements when enabled and database is empty")
    void shouldSeedEntitiesWhenEnabled() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ReflectionTestUtils.setField(demoDataSeeder, "seedDemoData", true);

        when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        Role managerRole = Role.builder().name("MANAGER").build();
        Role employeeRole = Role.builder().name("EMPLOYEE").build();
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");

        when(userRepository.findByEmail("manager@smartwarehouse.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("employee@smartwarehouse.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@smartwarehouse.com")).thenReturn(Optional.of(User.builder().email("admin@smartwarehouse.com").build()));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(categoryRepository.existsByName(anyString())).thenReturn(false);
        Category mockCat = Category.builder().id(UUID.randomUUID()).name("MockCat").build();
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.of(mockCat));

        when(supplierRepository.existsByName(anyString())).thenReturn(false);
        Supplier mockSup = Supplier.builder().id(UUID.randomUUID()).name("MockSup").build();
        when(supplierRepository.findByName(anyString())).thenReturn(Optional.of(mockSup));

        when(productRepository.existsBySku(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        when(notificationRepository.count()).thenReturn(0L);

        demoDataSeeder.run(null);

        verify(settingsRepository).save(any(AppSettings.class));
        verify(userRepository, times(2)).save(any(User.class));
        verify(categoryRepository, times(4)).save(any(Category.class));
        verify(supplierRepository, times(3)).save(any(Supplier.class));
        verify(productRepository, times(7)).save(any(Product.class));
        verify(movementService, atLeastOnce()).inbound(any(), any());
        verify(movementService, atLeastOnce()).outbound(any(), any());
        verify(notificationRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should be idempotent and skip duplicate records when data is already present")
    void shouldBeIdempotentWhenAlreadyPresent() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ReflectionTestUtils.setField(demoDataSeeder, "seedDemoData", true);

        when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(new AppSettings()));
        when(userRepository.findByEmail("manager@smartwarehouse.com")).thenReturn(Optional.of(new User()));
        when(userRepository.findByEmail("employee@smartwarehouse.com")).thenReturn(Optional.of(new User()));
        when(userRepository.findByEmail("admin@smartwarehouse.com")).thenReturn(Optional.of(new User()));

        when(categoryRepository.existsByName(anyString())).thenReturn(true);
        when(supplierRepository.existsByName(anyString())).thenReturn(true);
        when(productRepository.existsBySku(anyString())).thenReturn(true);
        when(notificationRepository.count()).thenReturn(5L);

        demoDataSeeder.run(null);

        verify(settingsRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(categoryRepository, never()).save(any());
        verify(supplierRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(movementService);
    }
}
