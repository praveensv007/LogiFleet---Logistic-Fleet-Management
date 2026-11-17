package com.logistics.config;

import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Default roles, permissions and the admin user; optional demo data (one user per role). */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    /** code, name, module */
    private static final String[][] PERMISSIONS = {
            {"USER_MANAGE", "Manage users", "USERS"},
            {"ROLE_MANAGE", "Manage roles and permissions", "USERS"},
            {"CUSTOMER_VIEW", "View customers", "CUSTOMERS"},
            {"CUSTOMER_EDIT", "Edit customers", "CUSTOMERS"},
            {"DRIVER_VIEW", "View drivers", "DRIVERS"},
            {"DRIVER_EDIT", "Create and edit drivers", "DRIVERS"},
            {"VEHICLE_VIEW", "View vehicles", "VEHICLES"},
            {"VEHICLE_EDIT", "Create and edit vehicles", "VEHICLES"},
            {"WAREHOUSE_EDIT", "Create and edit warehouses", "WAREHOUSES"},
            {"WAREHOUSE_OPS", "Receive, pack, load and dispatch", "WAREHOUSES"},
            {"SHIPMENT_CREATE", "Create shipments", "SHIPMENTS"},
            {"SHIPMENT_VIEW_ALL", "View all shipments", "SHIPMENTS"},
            {"SHIPMENT_EDIT", "Edit or cancel any shipment", "SHIPMENTS"},
            {"ASSIGNMENT_MANAGE", "Assign drivers and vehicles", "FLEET"},
            {"DELIVERY_UPDATE", "Accept trips and update deliveries", "FLEET"},
            {"DELIVERY_MONITOR", "Monitor live deliveries and problems", "FLEET"},
            {"MAINTENANCE_MANAGE", "Manage vehicle maintenance", "FLEET"},
            {"REPORT_VIEW", "View and export reports", "REPORTS"},
            {"AUDIT_VIEW", "View audit logs", "REPORTS"},
    };

    private static final Map<RoleName, List<String>> DEFAULTS = Map.of(
            RoleName.FLEET_MANAGER, List.of("CUSTOMER_VIEW", "DRIVER_VIEW", "DRIVER_EDIT", "VEHICLE_VIEW", "VEHICLE_EDIT",
                    "SHIPMENT_VIEW_ALL", "ASSIGNMENT_MANAGE", "DELIVERY_MONITOR", "MAINTENANCE_MANAGE", "REPORT_VIEW"),
            RoleName.WAREHOUSE_STAFF, List.of("WAREHOUSE_OPS", "SHIPMENT_VIEW_ALL", "VEHICLE_VIEW"),
            RoleName.DRIVER, List.of("DELIVERY_UPDATE"),
            RoleName.CUSTOMER, List.of("SHIPMENT_CREATE"));

    private static final Map<RoleName, String> DESCRIPTIONS = Map.of(
            RoleName.ADMIN, "Users, roles, all master data, reports, audit logs",
            RoleName.FLEET_MANAGER, "Vehicles, drivers, assignments, maintenance",
            RoleName.WAREHOUSE_STAFF, "Receive, pack, load and dispatch shipments",
            RoleName.DRIVER, "Accept trips, update delivery status, report problems",
            RoleName.CUSTOMER, "Create shipments, track live, delivery history");

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.seed.demo-data:false}")
    private boolean demoData;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permission> perms = new HashMap<>();
        for (String[] p : PERMISSIONS) {
            perms.put(p[0], permissionRepository.findByCode(p[0])
                    .orElseGet(() -> permissionRepository.save(new Permission(p[0], p[1], p[2]))));
        }
        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName rn : RoleName.values()) {
            Role role = roleRepository.findByName(rn.authority()).orElse(null);
            if (role == null) {
                role = new Role(rn.authority(), DESCRIPTIONS.get(rn));
                if (rn != RoleName.ADMIN) {
                    role.setPermissions(new HashSet<>(DEFAULTS.get(rn).stream().map(perms::get).toList()));
                }
                role = roleRepository.save(role);
            }
            if (rn == RoleName.ADMIN) {
                role.setPermissions(new HashSet<>(perms.values())); // admin always has everything
            }
            roles.put(rn, role);
        }

        if (!userRepository.existsByUsername(adminUsername)) {
            user(adminUsername, adminEmail, "9000000001", "System Administrator", adminPassword, roles.get(RoleName.ADMIN), null);
            log.info("Created admin user '{}' (password from app.admin.password)", adminUsername);
        }

        if (demoData && warehouseRepository.count() == 0) {
            seedDemo(roles);
        }
    }

    private void seedDemo(Map<RoleName, Role> roles) {
        String pwd = "Demo@1234";
        User staff = user("warehouse", "warehouse@logistics.local", "9000000003", "Priya Warehouse", pwd,
                roles.get(RoleName.WAREHOUSE_STAFF), null);

        Warehouse w = new Warehouse();
        w.setWarehouseCode("WH-CHN-01");
        w.setName("Chennai Central Hub");
        w.setAddressLine1("12 GST Road, Guindy");
        w.setCity("Chennai");
        w.setState("Tamil Nadu");
        w.setPincode("600032");
        w.setCountry("India");
        w.setLatitude(new BigDecimal("13.0067000"));
        w.setLongitude(new BigDecimal("80.2206000"));
        w.setManager(staff);
        w.setContactPhone("9000000010");
        w.setContactEmail("chennai.hub@logistics.local");
        w.setCapacity(50000);
        w.setOpeningTime(LocalTime.of(8, 0));
        w.setClosingTime(LocalTime.of(20, 0));
        w.setStatus(ActiveStatus.ACTIVE);
        warehouseRepository.save(w);
        staff.setWarehouse(w);

        user("fleet", "fleet@logistics.local", "9000000002", "Ravi Fleet", pwd, roles.get(RoleName.FLEET_MANAGER), null);

        User driverUser = user("driver", "driver@logistics.local", "9000000004", "Kumar Driver", pwd,
                roles.get(RoleName.DRIVER), null);
        Driver d = new Driver();
        d.setUser(driverUser);
        d.setFullName(driverUser.getFullName());
        d.setDateOfBirth(LocalDate.of(1990, 5, 14));
        d.setGender(Gender.MALE);
        d.setPhone(driverUser.getPhone());
        d.setAddress("45 Anna Nagar");
        d.setCity("Chennai");
        d.setState("Tamil Nadu");
        d.setPincode("600040");
        d.setLicenseNumber("TN0920150012345");
        d.setLicenseType(LicenseType.TRANSPORT);
        d.setLicenseIssueDate(LocalDate.of(2015, 3, 1));
        d.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        d.setLicenseDocUrl("/api/files/sample-license.pdf");
        d.setIdProofType(IdProofType.AADHAAR);
        d.setIdProofNumber("123412341234");
        d.setExperienceYears(9);
        d.setJoiningDate(LocalDate.of(2020, 1, 10));
        d.setEmergencyContactName("Lakshmi");
        d.setEmergencyContactPhone("9000000011");
        d.setAvailabilityStatus(DriverAvailability.AVAILABLE);
        driverRepository.save(d);
        d.setDriverCode(String.format("DRV-%04d", d.getId()));

        Vehicle v = new Vehicle();
        v.setVehicleNumber("TN09AB1234");
        v.setVehicleType(VehicleType.MINI_TRUCK);
        v.setMake("Tata");
        v.setModel("Ace");
        v.setManufactureYear(2022);
        v.setFuelType(FuelType.DIESEL);
        v.setCapacityKg(new BigDecimal("750"));
        v.setCapacityCbm(new BigDecimal("4.50"));
        v.setChassisNumber("MAT445566CHS0001");
        v.setEngineNumber("ENG4455660001");
        v.setInsuranceNumber("INS-2026-0001");
        v.setInsuranceExpiry(LocalDate.now().plusYears(1));
        v.setPermitExpiry(LocalDate.now().plusYears(1));
        v.setFitnessExpiry(LocalDate.now().plusYears(1));
        v.setPucExpiry(LocalDate.now().plusMonths(5));
        v.setOdometerKm(18500);
        v.setHomeWarehouse(w);
        v.setNextServiceDate(LocalDate.now().plusDays(20));
        v.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(v);

        User custUser = user("customer", "customer@logistics.local", "9000000005", "Anitha Customer", pwd,
                roles.get(RoleName.CUSTOMER), null);
        Customer c = new Customer();
        c.setUser(custUser);
        c.setFullName(custUser.getFullName());
        c.setEmail(custUser.getEmail());
        c.setPhone(custUser.getPhone());
        c.setCustomerType(CustomerType.INDIVIDUAL);
        c.setAddressLine1("7 Cathedral Road");
        c.setCity("Chennai");
        c.setState("Tamil Nadu");
        c.setPincode("600086");
        c.setCountry("India");
        customerRepository.save(c);
        c.setCustomerCode(String.format("CUS%06d", c.getId()));

        log.info("Demo data created. Logins (password {}): fleet, warehouse, driver, customer", pwd);
    }

    private User user(String username, String email, String phone, String name, String password, Role role, Warehouse w) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setFullName(name);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setStatus(UserStatus.ACTIVE);
        u.setRoles(new HashSet<>(Set.of(role)));
        u.setWarehouse(w);
        return userRepository.save(u);
    }
}
