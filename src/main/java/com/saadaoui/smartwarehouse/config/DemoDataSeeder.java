package com.saadaoui.smartwarehouse.config;

import com.saadaoui.smartwarehouse.auth.entity.Role;
import com.saadaoui.smartwarehouse.auth.entity.User;
import com.saadaoui.smartwarehouse.auth.repository.RoleRepository;
import com.saadaoui.smartwarehouse.auth.repository.UserRepository;
import com.saadaoui.smartwarehouse.category.repository.CategoryRepository;
import com.saadaoui.smartwarehouse.entity.AppSettings;
import com.saadaoui.smartwarehouse.entity.Category;
import com.saadaoui.smartwarehouse.entity.MovementType;
import com.saadaoui.smartwarehouse.entity.Notification;
import com.saadaoui.smartwarehouse.entity.Product;
import com.saadaoui.smartwarehouse.entity.Supplier;
import com.saadaoui.smartwarehouse.movement.dto.MovementRequest;
import com.saadaoui.smartwarehouse.movement.service.MovementService;
import com.saadaoui.smartwarehouse.notification.NotificationTypes;
import com.saadaoui.smartwarehouse.notification.repository.NotificationRepository;
import com.saadaoui.smartwarehouse.product.repository.ProductRepository;
import com.saadaoui.smartwarehouse.settings.repository.AppSettingsRepository;
import com.saadaoui.smartwarehouse.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Opt-in demo data seeder for local development and demonstration environments.
 * Strictly blocked in production profiles.
 * Idempotent: checks for existing entities before saving to prevent duplicates.
 */
@Slf4j
@Component
@Order(2)
@Profile("!prod")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final Environment environment;
    private final AppSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final MovementService movementService;
    private final NotificationRepository notificationRepository;

    @Value("${application.seed-demo-data:false}")
    private boolean seedDemoData;

    @Value("${application.demo-data.manager-email:manager@smartwarehouse.com}")
    private String managerEmail;

    @Value("${application.demo-data.manager-password:Manager@123}")
    private String managerPassword;

    @Value("${application.demo-data.employee-email:employee@smartwarehouse.com}")
    private String employeeEmail;

    @Value("${application.demo-data.employee-password:Employee@123}")
    private String employeePassword;

    @Override
    public void run(ApplicationArguments args) {
        if (isProductionEnvironment()) {
            log.warn("Demo data seeding is strictly prohibited in production profiles. Skipping.");
            return;
        }

        if (!seedDemoData) {
            log.info("Demo data seeding is disabled (application.seed-demo-data=false).");
            return;
        }

        log.info("SEED_DEMO_DATA is enabled. Beginning idempotent sample business seeding...");

        seedAppSettings();
        User manager = seedUser(managerEmail, managerPassword, "Sarah", "Connor", "+1-555-0192", "MANAGER");
        User employee = seedUser(employeeEmail, employeePassword, "Alex", "Rivers", "+1-555-0144", "EMPLOYEE");
        User admin = userRepository.findByEmail("admin@smartwarehouse.com").orElse(null);

        seedCategories();
        seedSuppliers();
        seedProductsAndMovements();
        seedNotifications(admin, manager, employee);

        log.info("Demo data seeding completed successfully.");
    }

    private boolean isProductionEnvironment() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
    }

    private void seedAppSettings() {
        if (settingsRepository.findTopByOrderByIdAsc().isPresent()) {
            log.debug("AppSettings already present. Preserving existing settings.");
            return;
        }

        AppSettings settings = AppSettings.builder()
                .warehouseName("Apex Central Logistics Hub")
                .currency("USD")
                .lowStockThreshold(15)
                .notificationsEnabled(true)
                .build();

        settingsRepository.save(settings);
        log.info("Seeded initial AppSettings: {}", settings.getWarehouseName());
    }

    private User seedUser(String email, String rawPassword, String firstName, String lastName, String phone, String roleName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found during seeding"));

            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phone)
                    .roles(Set.of(role))
                    .enabled(true)
                    .accountNonLocked(true)
                    .accountNonExpired(true)
                    .credentialsNonExpired(true)
                    .build();

            User saved = userRepository.save(user);
            log.info("Seeded demo user [{}] with role {}", email, roleName);
            return saved;
        });
    }

    private void seedCategories() {
        List<CategorySeed> categories = List.of(
                new CategorySeed("Sensors & Vision Systems", "Industrial proximity, vision, and optical sensors"),
                new CategorySeed("Microcontrollers & SBCs", "Single-board computers, microcontrollers, and dev boards"),
                new CategorySeed("Motors & Motion Control", "Stepper motors, servo drives, and robotic actuators"),
                new CategorySeed("Industrial Cabling", "Heavy-duty shielded, ethernet, and power transmission cabling")
        );

        for (CategorySeed cat : categories) {
            if (!categoryRepository.existsByName(cat.name())) {
                Category category = Category.builder()
                        .name(cat.name())
                        .description(cat.description())
                        .active(true)
                        .build();
                categoryRepository.save(category);
                log.info("Seeded Category: {}", cat.name());
            }
        }
    }

    private void seedSuppliers() {
        List<SupplierSeed> suppliers = List.of(
                new SupplierSeed("NovaTech Electronics", "Elena Vance", "supplies@novatech-elec.example.com",
                        "+1-408-555-0188", "1042 Innovation Way, San Jose, CA 95134"),
                new SupplierSeed("Apex Motion Systems", "Marcus Sterling", "orders@apexmotion.example.com",
                        "+1-312-555-0162", "450 Industrial Pkwy, Chicago, IL 60607"),
                new SupplierSeed("Global Wire & Cable Corp", "David Chen", "sales@globalwire.example.com",
                        "+1-214-555-0175", "880 Commerce Blvd, Dallas, TX 75201")
        );

        for (SupplierSeed sup : suppliers) {
            if (!supplierRepository.existsByName(sup.name())) {
                Supplier supplier = Supplier.builder()
                        .name(sup.name())
                        .contactName(sup.contactName())
                        .email(sup.email())
                        .phone(sup.phone())
                        .address(sup.address())
                        .active(true)
                        .build();
                supplierRepository.save(supplier);
                log.info("Seeded Supplier: {}", sup.name());
            }
        }
    }

    private void seedProductsAndMovements() {
        List<ProductSeedDefinition> products = List.of(
                // 1. Healthy Stock: Qty 120, MinStock 25
                new ProductSeedDefinition(
                        "SBC-RPI4-8GB",
                        "Raspberry Pi 4 Model B (8GB RAM)",
                        "High performance quad-core 64-bit single board computer",
                        new BigDecimal("75.00"),
                        25,
                        "Microcontrollers & SBCs",
                        "NovaTech Electronics",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 150, "Initial bulk procurement PO-2026-101", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 30, "Dispatched to Assembly Line A", employeeEmail)
                        )
                ),
                // 2. Healthy Stock: Qty 85, MinStock 20
                new ProductSeedDefinition(
                        "CBL-CAT6-50M",
                        "Cat6 Industrial Shielded Cable (50m)",
                        "Heavy-duty outdoor UV-resistant STP cable spool",
                        new BigDecimal("29.99"),
                        20,
                        "Industrial Cabling",
                        "Global Wire & Cable Corp",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 100, "Warehouse stock replenishment", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 15, "Internal network infrastructure expansion", employeeEmail)
                        )
                ),
                // 3. Near-Low Stock: Qty 18, MinStock 15 (Approaching threshold)
                new ProductSeedDefinition(
                        "PWR-DIN-24V10A",
                        "24V 10A Industrial DIN Rail Power Supply",
                        "Ultra-slim AC-DC DIN rail mounted power supply unit",
                        new BigDecimal("64.50"),
                        15,
                        "Motors & Motion Control",
                        "NovaTech Electronics",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 30, "Scheduled quarterly intake", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 12, "Fulfilled control cabinet order #4910", employeeEmail)
                        )
                ),
                // 4. Low Stock: Qty 6, MinStock 15 (Triggers low-stock alert)
                new ProductSeedDefinition(
                        "MTR-NEMA23-28",
                        "NEMA 23 High-Torque Stepper Motor (2.8Nm)",
                        "Precision hybrid stepper motor for CNC and robotics",
                        new BigDecimal("42.00"),
                        15,
                        "Motors & Motion Control",
                        "Apex Motion Systems",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 40, "Initial batch from Apex Motion", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 34, "Bulk fulfillment for CNC robot build #114", employeeEmail)
                        )
                ),
                // 5. Low Stock: Qty 3, MinStock 8 (Triggers low-stock alert)
                new ProductSeedDefinition(
                        "CAM-IND-5MP",
                        "5MP Industrial GigE Machine Vision Camera",
                        "Monochrome global shutter sensor for automated inspection",
                        new BigDecimal("185.00"),
                        8,
                        "Sensors & Vision Systems",
                        "NovaTech Electronics",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 15, "Factory automation batch", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 12, "Installed on Quality Inspection Station 3", employeeEmail)
                        )
                ),
                // 6. Zero Stock / Out of Stock: Qty 0, MinStock 10
                new ProductSeedDefinition(
                        "SNS-US-IP67",
                        "IP67 Ultrasonic Proximity Sensor",
                        "Ruggedized distance transducer with analog 0-10V output",
                        new BigDecimal("22.50"),
                        10,
                        "Sensors & Vision Systems",
                        "NovaTech Electronics",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 25, "Restock batch PO-2026-088", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 25, "High-priority conveyor retrofit job", employeeEmail)
                        )
                ),
                // 7. High Velocity Reorder Flagged: Qty 14, MinStock 10, Outbound 46 units
                new ProductSeedDefinition(
                        "MCU-STM32-F4",
                        "STM32F407 ARM Cortex-M4 Development Board",
                        "Embedded controller board with 168MHz MCU and DSP",
                        new BigDecimal("34.00"),
                        10,
                        "Microcontrollers & SBCs",
                        "NovaTech Electronics",
                        List.of(
                                new MovementStep(MovementType.INBOUND, 60, "Initial quarterly lot", managerEmail),
                                new MovementStep(MovementType.OUTBOUND, 46, "Issued for Smart Sensor IoT nodes", employeeEmail)
                        )
                )
        );

        for (ProductSeedDefinition def : products) {
            if (productRepository.existsBySku(def.sku())) {
                log.debug("Product with SKU {} already exists. Skipping.", def.sku());
                continue;
            }

            Category category = categoryRepository.findByName(def.categoryName())
                    .orElseThrow(() -> new IllegalStateException("Category " + def.categoryName() + " not found"));
            Supplier supplier = supplierRepository.findByName(def.supplierName())
                    .orElse(null);

            Product product = Product.builder()
                    .sku(def.sku())
                    .name(def.name())
                    .description(def.description())
                    .price(def.price())
                    .quantity(0) // Start at zero; movements will step-by-step update to target
                    .minStock(def.minStock())
                    .category(category)
                    .supplier(supplier)
                    .active(true)
                    .build();

            Product savedProduct = productRepository.save(product);
            log.info("Created initial product [{}] with 0 quantity", savedProduct.getSku());

            // Run movements through domain service to guarantee audit logs and quantity integrity
            for (MovementStep step : def.movements()) {
                MovementRequest request = new MovementRequest();
                request.setProductId(savedProduct.getId());
                request.setQuantity(step.quantity());
                request.setReason(step.reason());

                if (step.type() == MovementType.INBOUND) {
                    movementService.inbound(request, step.performedBy());
                } else if (step.type() == MovementType.OUTBOUND) {
                    movementService.outbound(request, step.performedBy());
                } else if (step.type() == MovementType.ADJUSTMENT) {
                    movementService.adjust(request, step.performedBy());
                }
            }
        }
    }

    private void seedNotifications(User admin, User manager, User employee) {
        if (notificationRepository.count() > 0) {
            log.debug("Notifications already exist. Skipping notification seeding.");
            return;
        }

        if (manager == null || employee == null) {
            log.warn("Manager or Employee user missing. Skipping notifications.");
            return;
        }

        List<Notification> demoNotifications = List.of(
                Notification.builder()
                        .sender(admin)
                        .recipient(manager)
                        .title("Welcome to Apex Central Logistics Hub")
                        .message("Warehouse management system initialized. Please review default stock thresholds.")
                        .type(NotificationTypes.USER_MESSAGE)
                        .isRead(true)
                        .build(),
                Notification.builder()
                        .sender(manager)
                        .recipient(employee)
                        .title("Restock PO-2026-101 Verified")
                        .message("Raspberry Pi 4 Model B inbound shipment of 150 units has been received and inspected.")
                        .type(NotificationTypes.USER_MESSAGE)
                        .isRead(false)
                        .build(),
                Notification.builder()
                        .sender(employee)
                        .recipient(manager)
                        .title("Urgent: Ultrasonic Proximity Sensor Depleted")
                        .message("Stock for IP67 Ultrasonic Proximity Sensor (SNS-US-IP67) reached 0 units. Reorder recommended.")
                        .type(NotificationTypes.STOCK_ALERT)
                        .isRead(false)
                        .build()
        );

        notificationRepository.saveAll(demoNotifications);
        log.info("Seeded {} sample notifications", demoNotifications.size());
    }

    // Helper records for structured seed configuration
    private record CategorySeed(String name, String description) {}
    private record SupplierSeed(String name, String contactName, String email, String phone, String address) {}
    private record MovementStep(MovementType type, int quantity, String reason, String performedBy) {}
    private record ProductSeedDefinition(
            String sku,
            String name,
            String description,
            BigDecimal price,
            int minStock,
            String categoryName,
            String supplierName,
            List<MovementStep> movements
    ) {}
}
