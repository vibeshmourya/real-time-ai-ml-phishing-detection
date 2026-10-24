package com.phishshield.config;

import com.phishshield.domain.*;
import com.phishshield.repository.PreventionContentRepository;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.SecurityAlertRepository;
import com.phishshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PreventionContentRepository preventionContentRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.core.env.Environment environment;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PreventionContentRepository preventionContentRepository,
                           SecurityAlertRepository securityAlertRepository,
                           PasswordEncoder passwordEncoder,
                           org.springframework.core.env.Environment environment) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.preventionContentRepository = preventionContentRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsers();
        seedPreventionContent();
        seedSecurityAlerts();
    }

    private void seedRoles() {
        createRoleIfNotFound(RoleName.ROLE_USER, "Standard platform user with scanning, history, and reporting permissions");
        createRoleIfNotFound(RoleName.ROLE_SECURITY_ANALYST, "Security analyst with report triage and threat intelligence access");
        createRoleIfNotFound(RoleName.ROLE_ADMIN, "Platform administrator with full system, user, and configuration access");
    }

    private Role createRoleIfNotFound(RoleName name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            return roleRepository.save(role);
        });
    }

    private void seedUsers() {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean seedExplicitly = Boolean.parseBoolean(environment.getProperty("app.security.seed-demo-users", "false"));

        if (isProduction && !seedExplicitly) {
            log.info("Production environment active: Default demo accounts are disabled for production security.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElse(null);
        Role analystRole = roleRepository.findByName(RoleName.ROLE_SECURITY_ANALYST).orElse(null);
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElse(null);

        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@phishshield.security")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .fullName("System Administrator")
                    .role(adminRole)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default admin user: admin [development mode]");
        }

        if (!userRepository.existsByUsername("analyst")) {
            User analyst = User.builder()
                    .username("analyst")
                    .email("analyst@phishshield.security")
                    .passwordHash(passwordEncoder.encode("Analyst@123456"))
                    .fullName("Cyber Threat Analyst")
                    .role(analystRole)
                    .isActive(true)
                    .build();
            userRepository.save(analyst);
            log.info("Initialized default analyst user: analyst [development mode]");
        }

        if (!userRepository.existsByUsername("demo_user")) {
            User user = User.builder()
                    .username("demo_user")
                    .email("user@phishshield.security")
                    .passwordHash(passwordEncoder.encode("User@123456"))
                    .fullName("Alex Digital Citizen")
                    .role(userRole)
                    .isActive(true)
                    .build();
            userRepository.save(user);
            log.info("Initialized default demo user: demo_user [development mode]");
        }
    }

    private void seedPreventionContent() {
        createPreventionIfNotFound(
                ThreatCategory.BANKING_PHISHING,
                "Banking Phishing Defense Guide",
                "Attackers forge duplicate online banking portals to steal your internet banking user ID, password, OTP, and debit card PIN.",
                "Victims receive urgent SMS or emails claiming an account freeze or unauthorized debit. The link routes to a replica bank portal that captures credentials.",
                "Complete unauthorized financial drain, fraudulent loans, and identity theft.",
                "1. Never enter bank credentials or OTPs on links sent via SMS/Email.\n2. Always verify by manually typing the official bank URL.\n3. Immediately freeze card/account via official mobile app if leaked.",
                "[\"Do not enter OTP or PIN on unsolicited links\", \"Inspect domain for subtle misspellings\", \"Call the official fraud hotline on your physical card\"]",
                "/media/videos/banking-phishing.mp4",
                "/media/audio/banking-phishing-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.CREDENTIAL_THEFT,
                "Account Credential Theft Prevention",
                "Phishers build counterfeit login screens for Google, Microsoft 365, Apple, or corporate portals to hijack accounts.",
                "A fake document notification or password expiration email prompts the user to log in on a look-alike authentication page.",
                "Unauthorized email access, corporate data breaches, ransomware delivery, and private identity exposure.",
                "1. Enable App-Based Multi-Factor Authentication.\n2. Review active logged-in sessions in account settings.\n3. Change passwords immediately from a trusted device.",
                "[\"Verify URL bar displays genuine domain\", \"Look for password manager auto-fill warning\", \"Enable 2FA on all accounts\"]",
                "/media/videos/credential-theft.mp4",
                "/media/audio/credential-theft-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.FAKE_KYC,
                "Fake KYC Update Scam Advisory",
                "Scammers claim your SIM card, bank account, or wallet KYC has expired to trick you into downloading remote access apps or entering debit card details.",
                "The user receives an SMS: 'Dear customer, your KYC is pending, your SIM/A/c will be blocked today. Click here to verify.'",
                "Cybercriminals initiate unauthorized fund transfers or install screen-sharing software.",
                "1. No bank or telecom operator completes KYC via random SMS links.\n2. Visit the nearest official branch or use the verified official app.\n3. Block and report the sender phone number.",
                "[\"Telecom companies never threaten immediate disconnection via SMS links\", \"Never share OTP under any circumstance\", \"Never install remote sharing tools on unknown requests\"]",
                "/media/videos/fake-kyc.mp4",
                "/media/audio/fake-kyc-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.SHOPPING_SCAM,
                "Fake E-Commerce & Deep Discount Scam Guide",
                "Fraudulent shopping websites lure victims with impossible 90% discounts on iPhones, laptops, and branded items.",
                "Victims pay online upfront through fake payment gateways; goods are never shipped and credit card data is compromised.",
                "Direct financial loss and repeated unauthorized credit card recurring charges.",
                "1. If the price is too good to be true, it is almost certainly a scam.\n2. Verify the website reputation, domain age, and physical contact address.\n3. Use credit cards with fraud chargeback protection.",
                "[\"Check domain registration details\", \"Look for verified return and refund policies\", \"Avoid upfront payment via direct wire transfer\"]",
                "/media/videos/shopping-scam.mp4",
                "/media/audio/shopping-scam-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.JOB_SCAM,
                "Employment & Work-From-Home Scam Warning",
                "Fraudsters pose as recruiters offering high-paying part-time rating jobs, requiring registration fees or deposit tasks.",
                "Victims are asked to complete initial tasks, then pressured into depositing money into crypto wallets to unlock earnings.",
                "Loss of deposited funds and identity document theft through fake background verification forms.",
                "1. Legitimate employers never ask candidates to pay money for job offers.\n2. Research the company through official LinkedIn or corporate portals.\n3. Never submit banking passwords or pay security deposits.",
                "[\"No legitimate recruiter charges registration or training fees\", \"Cross-check job ID on company official careers page\", \"Never transfer funds to unlock work earnings\"]",
                "/media/videos/job-scam.mp4",
                "/media/audio/job-scam-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.DELIVERY_SCAM,
                "Courier & Package Delivery Scam Notice",
                "Fake SMS alerts from 'India Post / FedEx / DHL / UPS' claiming a package cannot be delivered due to incomplete address.",
                "The SMS directs victims to pay a nominal fee on a phishing portal that steals full card credentials.",
                "The nominal charge is an illusion; attackers drain full card limits.",
                "1. Check the tracking ID directly on the official postal or courier website.\n2. Do not click links in unsolicited delivery messages.\n3. Postal services do not demand card payments via SMS.",
                "[\"Open browser and navigate to courier official website manually\", \"Do not enter card CVV or OTP for package redelivery\", \"Report malicious delivery SMS to 1930\"]",
                "/media/videos/delivery-scam.mp4",
                "/media/audio/delivery-scam-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.GOVERNMENT_IMPERSONATION,
                "Government & Tax Agency Impersonation Advisory",
                "Scammers mimic tax departments, court notices, or subsidy boards offering refunds or threatening legal prosecution.",
                "Deceptive tax refund portals request taxpayer credentials, bank account numbers, and OTP authorizations.",
                "Tax fraud, identity theft, and severe financial losses.",
                "1. Official government portals only use verified government domains.\n2. Access government tax portals directly via official bookmarks.\n3. Government departments do not ask for debit card PINs for refunds.",
                "[\"Check domain ends with legitimate government TLDs\", \"Government agencies never contact citizens via WhatsApp\", \"Tax refunds are credited directly to verified bank accounts without OTP input\"]",
                "/media/videos/gov-scam.mp4",
                "/media/audio/gov-scam-en.mp3"
        );

        createPreventionIfNotFound(
                ThreatCategory.BENIGN,
                "Safe Browsing Best Practices",
                "The scanned URL does not display characteristic indicators of known phishing campaigns.",
                "Always continue practicing proactive cyber hygiene while browsing.",
                "Helps maintain proactive security posture.",
                "1. Keep your web browser and operating system updated.\n2. Use a modern password manager with unique passwords for every site.\n3. Always look for genuine HTTPS and domain spelling before entering sensitive credentials.",
                "[\"Verify domain spelling\", \"Use unique passwords for each service\", \"Keep multi-factor authentication enabled\"]",
                "/media/videos/safe-browsing.mp4",
                "/media/audio/safe-browsing-en.mp3"
        );
    }

    private void createPreventionIfNotFound(ThreatCategory category, String title, String shortExplanation,
                                           String attackMechanism, String whyItMatters, String immediateActions,
                                           String checklistJson, String videoUrl, String audioUrl) {
        if (preventionContentRepository.findByThreatCategoryAndLanguageCode(category, "en").isEmpty()) {
            PreventionContent content = PreventionContent.builder()
                    .threatCategory(category)
                    .title(title)
                    .shortExplanation(shortExplanation)
                    .attackMechanism(attackMechanism)
                    .whyItMatters(whyItMatters)
                    .immediateActions(immediateActions)
                    .checklistJson(checklistJson)
                    .videoUrl(videoUrl)
                    .audioUrl(audioUrl)
                    .languageCode("en")
                    .build();
            preventionContentRepository.save(content);
        }
    }

    private void seedSecurityAlerts() {
        if (securityAlertRepository.count() == 0) {
            SecurityAlert alert1 = SecurityAlert.builder()
                    .title("Surge in Fake Tax Refund Phishing SMS Campaigns")
                    .alertType(AlertType.EMERGING_THREAT)
                    .severity(AlertSeverity.CRITICAL)
                    .message("A large-scale SMS phishing campaign is impersonating national tax portals offering instant refunds. Do not click links claiming tax refunds requiring card details.")
                    .targetAudience("ALL")
                    .isActive(true)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            SecurityAlert alert2 = SecurityAlert.builder()
                    .title("Look-Alike Banking Domains Exploiting Free TLDs (.tk, .xyz)")
                    .alertType(AlertType.ADVISORY)
                    .severity(AlertSeverity.WARNING)
                    .message("Attackers are registering punycode and typo-squatted domains targeting major commercial banks. Ensure netbanking URLs are typed directly.")
                    .targetAudience("ALL")
                    .isActive(true)
                    .expiresAt(LocalDateTime.now().plusDays(60))
                    .build();

            SecurityAlert alert3 = SecurityAlert.builder()
                    .title("Work-From-Home Telegram Task Scams Targeting Students")
                    .alertType(AlertType.ADVISORY)
                    .severity(AlertSeverity.WARNING)
                    .message("Fraudulent part-time video rating and review jobs are extorting money from college students. Remember: legitimate employers never charge candidate fees.")
                    .targetAudience("STUDENTS")
                    .isActive(true)
                    .expiresAt(LocalDateTime.now().plusDays(45))
                    .build();

            securityAlertRepository.save(alert1);
            securityAlertRepository.save(alert2);
            securityAlertRepository.save(alert3);
            log.info("Seeded initial security advisories");
        }
    }
}
