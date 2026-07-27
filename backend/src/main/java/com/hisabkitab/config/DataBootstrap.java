package com.hisabkitab.config;

import com.hisabkitab.domain.AppUser;
import com.hisabkitab.domain.Attendance;
import com.hisabkitab.domain.AttendanceStatus;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.Employer;
import com.hisabkitab.domain.EntryType;
import com.hisabkitab.domain.LedgerEntry;
import com.hisabkitab.domain.Organization;
import com.hisabkitab.domain.Role;
import com.hisabkitab.domain.WageRate;
import com.hisabkitab.repository.AppUserRepository;
import com.hisabkitab.repository.AttendanceRepository;
import com.hisabkitab.repository.EmployeeRepository;
import com.hisabkitab.repository.EmployerRepository;
import com.hisabkitab.repository.LedgerEntryRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.repository.WageRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Creates the first organization and owner login on an empty database, so a
 * fresh checkout can be signed into without any manual SQL.
 */
@Component
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    private final AppProperties properties;
    private final OrganizationRepository organizations;
    private final EmployerRepository employers;
    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final WageRateRepository wageRates;
    private final AttendanceRepository attendance;
    private final LedgerEntryRepository ledger;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrap(AppProperties properties,
                         OrganizationRepository organizations,
                         EmployerRepository employers,
                         AppUserRepository users,
                         EmployeeRepository employees,
                         WageRateRepository wageRates,
                         AttendanceRepository attendance,
                         LedgerEntryRepository ledger,
                         PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.organizations = organizations;
        this.employers = employers;
        this.users = users;
        this.employees = employees;
        this.wageRates = wageRates;
        this.attendance = attendance;
        this.ledger = ledger;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Bootstrap config = properties.getBootstrap();
        if (!config.isEnabled() || organizations.count() > 0) {
            return;
        }

        Organization org = new Organization();
        org.setName(config.getOrganizationName());
        org = organizations.save(org);

        Employer employer = new Employer();
        employer.setOrganization(org);
        employer.setName(config.getOwnerName());
        employer = employers.save(employer);

        AppUser owner = new AppUser();
        owner.setOrganization(org);
        owner.setEmployer(employer);
        owner.setUsername(config.getUsername());
        owner.setPasswordHash(passwordEncoder.encode(config.getPassword()));
        owner.setDisplayName(config.getOwnerName());
        owner.setRole(Role.OWNER);
        users.save(owner);

        log.info("Created organization '{}' with owner login '{}'", org.getName(), owner.getUsername());

        if (config.isSeedDemoData()) {
            seedDemo(org, employer, owner);
        }
    }

    private void seedDemo(Organization org, Employer employer, AppUser owner) {
        record Person(String code, String name, String village, int rate) {
        }

        List<Person> people = List.of(
                new Person("E01", "Ramesh Yadav", "Chandpur", 450),
                new Person("E02", "Sunita Devi", "Chandpur", 400),
                new Person("E03", "Mohan Lal", "Bharatpur", 500),
                new Person("E04", "Kavita Kumari", "Chandpur", 400),
                new Person("E05", "Dinesh Patel", "Rampur", 550),
                new Person("E06", "Anita Bai", "Rampur", 420),
                new Person("E07", "Suresh Meena", "Bharatpur", 480),
                new Person("E08", "Lakshmi Devi", "Chandpur", 400),
                new Person("E09", "Vijay Singh", "Rampur", 600),
                new Person("E10", "Geeta Sharma", "Bharatpur", 430));

        LocalDate today = LocalDate.now();
        LocalDate joined = today.minusMonths(8).withDayOfMonth(1);
        Random random = new Random(42);

        for (Person person : people) {
            Employee employee = new Employee();
            employee.setOrganization(org);
            employee.setCode(person.code());
            employee.setName(person.name());
            employee.setVillage(person.village());
            employee.setPhone("98" + (10000000 + random.nextInt(89999999)));
            employee.setDailyWageRate(BigDecimal.valueOf(person.rate()));
            employee.setJoinedOn(joined);
            employee = employees.save(employee);

            wageRates.save(new WageRate(employee, employee.getDailyWageRate(), joined, "Rate at joining"));

            // A couple of advances spread over the last few months.
            int advances = 1 + random.nextInt(3);
            for (int i = 0; i < advances; i++) {
                LocalDate date = today.minusDays(20L + random.nextInt(150));
                if (date.isBefore(joined)) {
                    continue;
                }
                BigDecimal amount = BigDecimal.valueOf((2 + random.nextInt(18)) * 500L);
                save(org, employee, employer, owner, EntryType.ADVANCE, amount, date,
                        i == 0 ? "Advance for household expense" : "Advance requested");
            }

            // Occasional cash repayment.
            if (random.nextBoolean()) {
                LocalDate date = today.minusDays(5L + random.nextInt(40));
                save(org, employee, employer, owner, EntryType.REPAYMENT,
                        BigDecimal.valueOf((1 + random.nextInt(6)) * 500L), date, "Cash returned");
            }

            // Scatter absences across the current and previous month.
            for (int i = 0; i < 4 + random.nextInt(5); i++) {
                LocalDate date = today.minusDays(1L + random.nextInt(55));
                if (date.isBefore(joined)
                        || attendance.findByEmployeeIdAndWorkDate(employee.getId(), date).isPresent()) {
                    continue;
                }
                Attendance row = new Attendance();
                row.setOrganization(org);
                row.setEmployee(employee);
                row.setWorkDate(date);
                row.setStatus(random.nextInt(10) < 7
                        ? AttendanceStatus.ABSENT
                        : AttendanceStatus.HALF_DAY);
                attendance.save(row);
            }
        }

        log.info("Seeded {} demo employees with advances and attendance", people.size());
    }

    private void save(Organization org, Employee employee, Employer employer, AppUser creator,
                      EntryType type, BigDecimal amount, LocalDate date, String note) {
        LedgerEntry entry = new LedgerEntry();
        entry.setOrganization(org);
        entry.setEmployee(employee);
        entry.setEmployer(employer);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setSignedAmount(type.sign() < 0 ? amount.negate() : amount);
        entry.setEntryDate(date);
        entry.setNote(note);
        entry.setCreatedBy(creator);
        ledger.save(entry);
    }
}
