package com.hackconnect.config;

import com.hackconnect.model.*;
import com.hackconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * DataSeeder — runs on startup.
 *
 * OPPORTUNITY FRESHNESS:
 * Deadlines are computed from NOW() every startup so they are always
 * in the future relative to when you start the app.
 * We DELETE all existing opportunities first so they are always fresh.
 * User accounts and roadmaps are preserved.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository            userRepository;
    private final OpportunityRepository     opportunityRepository;
    private final LearningRoadmapRepository roadmapRepository;
    private final RoadmapStepRepository     stepRepository;
    private final PasswordEncoder           passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Always re-seed opportunities with fresh dates
        refreshOpportunities();

        // Only seed users + roadmaps once
        if (userRepository.count() == 0) {
            seedUsers();
            seedRoadmaps();
            log.info("✅  Full seed complete.");
        } else {
            log.info("✅  Opportunities refreshed with current dates.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OPPORTUNITIES — deleted and re-seeded every startup for fresh deadlines
    // ─────────────────────────────────────────────────────────────────────────
    private void refreshOpportunities() {
        // Delete tags first (FK child), then opportunities (FK parent)
        // Cannot use deleteAll() directly — FK constraint on opportunity_tags
        try {
            opportunityRepository.deleteAllTags();
            opportunityRepository.deleteAllOpportunities();
        } catch (Exception e) {
            log.warn("Could not clear opportunities (first boot?): {}", e.getMessage());
        }

        User admin = userRepository.findByEmail("admin@hackconnect.dev")
                .orElse(null); // null on very first boot — set after seedUsers()

        seedOpportunities(admin);
        log.info("  \u2192 {} opportunities seeded with fresh dates",
                opportunityRepository.count());
    }

    private void seedOpportunities(User admin) {
        // admin may be null on first ever boot; we pass null and set it after users are seeded
        // The @ManyToOne nullable=true on postedBy handles this fine
        LocalDateTime now = LocalDateTime.now();

        opportunityRepository.saveAll(List.of(

                // ── HACKATHONS ────────────────────────────────────────────────
                opp(admin,
                        "Smart India Hackathon 2025",
                        "India's biggest national-level hackathon. Teams of 6 solve real government problem statements over 36 hours at nodal centres across India. Over 1 lakh students participate every year.",
                        Opportunity.Type.HACKATHON,
                        "Ministry of Education, Govt. of India",
                        false, "Pan India — multiple nodal centres",
                        "All Domains", "₹1,00,000 per winning team",
                        now.plusDays(38), now.plusDays(55),
                        "https://www.sih.gov.in",
                        new HashSet<>(Set.of("government","social-impact","national","team")), true, 1240),

                opp(admin,
                        "HackWithInfy 2025",
                        "Infosys hackathon open to all engineering students in India. Build innovative web, mobile, and AI solutions. Top teams get Pre-Placement Offers.",
                        Opportunity.Type.HACKATHON,
                        "Infosys",
                        true, null,
                        "Web Dev", "₹50,000 + Pre-Placement Offer (PPO)",
                        now.plusDays(14), now.plusDays(25),
                        "https://www.hackerearth.com/challenges/hackathon/hackwithinfy",
                        new HashSet<>(Set.of("corporate","web","ppo","online")), true, 892),

                opp(admin,
                        "HackRPI XII — MLH Official",
                        "24-hour MLH-sanctioned hackathon open to students worldwide. Hardware, software, and AI projects welcome. Mentors from top tech companies on-site.",
                        Opportunity.Type.HACKATHON,
                        "Rensselaer Polytechnic Institute (MLH)",
                        false, "Troy, New York, USA",
                        "All Domains", "Cash prizes + sponsor awards",
                        now.plusDays(22), now.plusDays(30),
                        "https://hackrpi.com",
                        new HashSet<>(Set.of("mlh","hardware","ai","international")), true, 430),

                opp(admin,
                        "HackCBS 8.0",
                        "Delhi's largest student-run hackathon at Shaheed Sukhdev College of Business Studies. 24-hour event, 1000+ participants, 50+ prizes.",
                        Opportunity.Type.HACKATHON,
                        "Shaheed Sukhdev College of Business Studies",
                        false, "Delhi, India",
                        "All Domains", "Prizes worth ₹5,00,000+",
                        now.plusDays(18), now.plusDays(28),
                        "https://hackcbs.tech",
                        new HashSet<>(Set.of("delhi","college","beginner-friendly")), true, 560),

                opp(admin,
                        "MLH Global Hack Week — Build",
                        "Major League Hacking's week-long online hackathon series. Build track: ship a working project each day. Beginner-friendly, free, global.",
                        Opportunity.Type.HACKATHON,
                        "Major League Hacking (MLH)",
                        true, null,
                        "All Domains", "Swag + certificates + GitHub achievements",
                        now.plusDays(7), now.plusDays(7),
                        "https://mlh.io/seasons/2025/events",
                        new HashSet<>(Set.of("mlh","online","beginner-friendly","free")), true, 780),

                // ── COMPETITIONS ──────────────────────────────────────────────
                opp(admin,
                        "ACM-ICPC Asia Regional 2025",
                        "Prestigious competitive programming contest. Top teams from Indian regionals qualify for the Asia Pacific Championship and World Finals.",
                        Opportunity.Type.COMPETITION,
                        "Association for Computing Machinery (ACM)",
                        false, "Multiple Indian Regionals",
                        "Competitive Programming", "Certificates + World Finals qualification",
                        now.plusDays(28), now.plusDays(42),
                        "https://icpc.global",
                        new HashSet<>(Set.of("acm","algorithms","competitive-programming")), true, 765),

                opp(admin,
                        "Google Code Jam 2025",
                        "Google's global algorithmic programming contest. Qualification → Round 1 → Round 2 → Round 3 → World Finals. Top 25 fly to Google HQ.",
                        Opportunity.Type.COMPETITION,
                        "Google",
                        true, null,
                        "Competitive Programming", "Top prize $15,000 USD",
                        now.plusDays(21), now.plusDays(35),
                        "https://codingcompetitions.withgoogle.com/codejam",
                        new HashSet<>(Set.of("google","algorithms","cp","global")), true, 980),

                opp(admin,
                        "Flipkart GRiD 6.0 — Software Development",
                        "Flipkart's national engineering challenge. Solve real e-commerce problems. Software development and robotics tracks. One of India's top corporate hackathons.",
                        Opportunity.Type.COMPETITION,
                        "Flipkart",
                        true, null,
                        "Software Development", "₹75,000 + internship opportunity",
                        now.plusDays(12), now.plusDays(20),
                        "https://unstop.com/hackathons/flipkart-grid",
                        new HashSet<>(Set.of("flipkart","ecommerce","india")), true, 1100),

                // ── INTERNSHIPS ───────────────────────────────────────────────
                opp(admin,
                        "Google Summer of Code 2025",
                        "Google's open source internship program. Work remotely for 12 weeks on a real open source organisation with an experienced mentor. One of the strongest open-source credentials for students globally.",
                        Opportunity.Type.INTERNSHIP,
                        "Google",
                        true, null,
                        "Open Source", "$3,000 – $6,600 stipend (varies by country)",
                        now.plusDays(10), now.plusDays(60),
                        "https://summerofcode.withgoogle.com",
                        new HashSet<>(Set.of("google","open-source","stipend","remote")), true, 2100),

                opp(admin,
                        "Microsoft Engage Mentorship 2025",
                        "Microsoft India's flagship mentorship programme for Indian engineering students. Build a real project under a Microsoft engineer mentor over 4 weeks. Past participants have received PPOs.",
                        Opportunity.Type.INTERNSHIP,
                        "Microsoft India",
                        true, null,
                        "Software Development", "Certificate + PPO chances",
                        now.plusDays(32), now.plusDays(60),
                        "https://microsoft.acehacker.com/engage",
                        new HashSet<>(Set.of("microsoft","mentorship","india","cloud")), true, 1350),

                opp(admin,
                        "Amazon ML Summer School 2025",
                        "Amazon's ML-focused programme for pre-final year students. Learn from Amazon scientists across 5 ML topics. Selected students get fast-tracked for Amazon internships.",
                        Opportunity.Type.INTERNSHIP,
                        "Amazon India",
                        true, null,
                        "AI/ML", "Free programme + Amazon internship fast-track",
                        now.plusDays(16), now.plusDays(45),
                        "https://amazonmlsummerschool.com",
                        new HashSet<>(Set.of("amazon","ml","india","summer-school")), true, 1620),

                opp(admin,
                        "Goldman Sachs Engineering Essentials",
                        "Goldman Sachs' 4-week virtual programme for pre-penultimate year students. Learn software engineering, data, and quant finance. Top performers get interview opportunities.",
                        Opportunity.Type.INTERNSHIP,
                        "Goldman Sachs",
                        true, null,
                        "Software Development", "Certificate + interview opportunity",
                        now.plusDays(24), now.plusDays(50),
                        "https://goldmansachs.com/careers/students",
                        new HashSet<>(Set.of("goldman-sachs","finance","engineering","virtual")), true, 890),

                // ── EVENTS ────────────────────────────────────────────────────
                opp(admin,
                        "Google I/O Extended India 2025",
                        "GDG India's nationwide community event following Google I/O. Watch talks, join codelabs on Android, Web, AI, and Cloud. Free entry, multiple cities.",
                        Opportunity.Type.EVENT,
                        "Google Developer Groups (GDG) India",
                        false, "Multiple cities — Delhi, Bangalore, Mumbai, Hyderabad",
                        "All Domains", null,
                        now.plusDays(9), now.plusDays(9),
                        "https://gdg.community.dev/events/",
                        new HashSet<>(Set.of("gdg","google","free","networking","workshop")), true, 430),

                opp(admin,
                        "GitHub Universe 2025",
                        "GitHub's annual developer conference. AI in dev workflows, Copilot deep dives, open source talks. Free online attendance. In-person in San Francisco.",
                        Opportunity.Type.EVENT,
                        "GitHub (Microsoft)",
                        true, null,
                        "All Domains", "Free online attendance",
                        now.plusDays(45), now.plusDays(45),
                        "https://githubuniverse.com",
                        new HashSet<>(Set.of("github","ai","devtools","open-source")), true, 670),

                opp(admin,
                        "AWS re:Invent Student Track 2025",
                        "Amazon Web Services' largest annual conference with a dedicated student track. Cloud fundamentals, certifications prep, and networking with AWS engineers.",
                        Opportunity.Type.EVENT,
                        "Amazon Web Services (AWS)",
                        true, null,
                        "DevOps", "Free online student pass",
                        now.plusDays(60), now.plusDays(60),
                        "https://reinvent.awsevents.com",
                        new HashSet<>(Set.of("aws","cloud","certification","free")), true, 520)
        ));
    }

    private Opportunity opp(User admin, String title, String desc,
                            Opportunity.Type type, String organizer, boolean online,
                            String location, String domain, String prize,
                            LocalDateTime deadline, LocalDateTime startDate,
                            String regUrl, Set<String> tags, boolean verified, int views) {
        return Opportunity.builder()
                .title(title).description(desc).type(type).organizer(organizer)
                .online(online).location(location).domain(domain).prize(prize)
                .deadline(deadline).startDate(startDate)
                .registrationUrl(regUrl).tags(tags).verified(verified)
                .viewCount(views).postedBy(admin).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USERS (first boot only)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedUsers() {
        userRepository.saveAll(List.of(
                User.builder()
                        .name("Admin HackConnect").email("admin@hackconnect.dev")
                        .password(passwordEncoder.encode("Admin@1234"))
                        .role(User.Role.ADMIN).college("HackConnect HQ").build(),
                User.builder()
                        .name("Priyansh Sharma").email("priyansh@iit.ac.in")
                        .password(passwordEncoder.encode("Student@1234"))
                        .role(User.Role.STUDENT).college("IIT Delhi").graduationYear("2025")
                        .skills(new HashSet<>(Set.of("React","Node.js","MongoDB")))
                        .interests(new HashSet<>(Set.of("Full Stack","Hackathons")))
                        .bio("Passionate full-stack developer.").build(),
                User.builder()
                        .name("Ananya Singh").email("ananya@bits.ac.in")
                        .password(passwordEncoder.encode("Student@1234"))
                        .role(User.Role.STUDENT).college("BITS Pilani").graduationYear("2026")
                        .skills(new HashSet<>(Set.of("Python","TensorFlow","Pandas")))
                        .interests(new HashSet<>(Set.of("AI/ML","Research"))).build()
        ));

        // Now that admin user exists, update opportunity postedBy
        User admin = userRepository.findByEmail("admin@hackconnect.dev").orElseThrow();
        opportunityRepository.findAll().forEach(o -> {
            o.setPostedBy(admin);
            opportunityRepository.save(o);
        });

        log.info("  \u2192 Users seeded");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROADMAPS (first boot only — content from roadmap.sh / nilbuild repo)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedRoadmaps() {
        seedFrontendRoadmap();
        seedBackendJavaRoadmap();
        seedFullStackRoadmap();
        seedMLRoadmap();
        seedDevOpsRoadmap();
        seedAndroidRoadmap();
        seedReactRoadmap();
        seedPythonRoadmap();
        log.info("  \u2192 {} roadmaps seeded", roadmapRepository.count());
    }

    private void seedFrontendRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("Frontend Development").domain("Frontend")
                .description("Complete path to becoming a frontend developer. Covers HTML, CSS, JavaScript, React, TypeScript, testing, and performance. Source: roadmap.sh/frontend")
                .level(LearningRoadmap.Level.BEGINNER).estimatedWeeks(16).enrolledCount(2100).build());
        steps(r,
                s(r,1,"How the Internet Works","DNS, HTTP/HTTPS, browsers, domain names, hosting. Knowing how data travels from server to browser is essential before writing a single line of code.",10,
                        l("https://developer.mozilla.org/en-US/docs/Learn/Common_questions/Web_mechanics/How_does_the_Internet_work","https://roadmap.sh/guides/what-is-internet"),
                        p("Draw a full request-response diagram","Write a one-page explainer: what happens when you type google.com")),
                s(r,2,"HTML","Semantic HTML5, forms, accessibility (a11y), SEO meta tags, browser DevTools. Source: roadmap.sh/html",18,
                        l("https://developer.mozilla.org/en-US/docs/Learn/HTML","https://web.dev/learn/html/"),
                        p("Semantic personal portfolio page","Multi-page website with accessible forms")),
                s(r,3,"CSS","Box model, Flexbox, CSS Grid, responsive design, animations, CSS variables. Source: roadmap.sh/css",25,
                        l("https://developer.mozilla.org/en-US/docs/Learn/CSS","https://web.dev/learn/css/","https://css-tricks.com/snippets/css/a-guide-to-flexbox/"),
                        p("Clone Netflix landing page (HTML+CSS only)","Responsive dashboard layout with CSS Grid")),
                s(r,4,"JavaScript","DOM, events, fetch API, async/await, ES6+. Source: roadmap.sh/javascript",35,
                        l("https://javascript.info","https://developer.mozilla.org/en-US/docs/Learn/JavaScript"),
                        p("Weather app using OpenWeatherMap API","Todo list with localStorage","Quiz app with timer")),
                s(r,5,"Git & GitHub","Commits, branches, merging, pull requests. Source: roadmap.sh/git-github",8,
                        l("https://learngitbranching.js.org","https://docs.github.com/en/get-started"),
                        p("GitHub profile README","Collaborate with a friend — open a PR")),
                s(r,6,"React","Components, hooks, React Router, Context API. Source: roadmap.sh/react",30,
                        l("https://react.dev/learn","https://roadmap.sh/react"),
                        p("Movie search app","Shopping cart with Context API")),
                s(r,7,"TypeScript","Types, interfaces, generics, utility types. Source: roadmap.sh/typescript",18,
                        l("https://www.typescriptlang.org/docs/handbook/intro.html","https://roadmap.sh/typescript"),
                        p("Rewrite weather app in TypeScript","Type-safe form library")),
                s(r,8,"Testing","Vitest, React Testing Library, Playwright E2E.",14,
                        l("https://vitest.dev/guide/","https://testing-library.com/docs/react-testing-library/intro/","https://playwright.dev/docs/intro"),
                        p("Unit tests for todo app","E2E test for login flow")),
                s(r,9,"Performance & Deployment","Core Web Vitals, lazy loading, code splitting, Vercel/Netlify deployment.",10,
                        l("https://web.dev/performance/","https://vercel.com/docs"),
                        p("Deploy React app on Vercel","Lighthouse audit — fix top 3 issues"))
        );
    }

    private void seedBackendJavaRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("Backend — Java & Spring Boot").domain("Backend")
                .description("Production-grade REST APIs: Java 17, Spring Boot 3, JWT auth, JPA/Hibernate, PostgreSQL, Docker. Source: roadmap.sh/java + roadmap.sh/spring-boot")
                .level(LearningRoadmap.Level.INTERMEDIATE).estimatedWeeks(18).enrolledCount(870).build());
        steps(r,
                s(r,1,"Java Fundamentals","OOP, generics, collections, streams, lambdas. Source: roadmap.sh/java",25,
                        l("https://dev.java/learn/","https://www.baeldung.com/java-tutorial","https://roadmap.sh/java"),
                        p("Bank account simulator","Library management CRUD")),
                s(r,2,"Maven","POM.xml, dependencies, lifecycle phases.",8,
                        l("https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html"),
                        p("Multi-module Maven project")),
                s(r,3,"Spring Boot Basics","IoC, DI, auto-configuration, profiles. Source: roadmap.sh/spring-boot",15,
                        l("https://docs.spring.io/spring-boot/docs/current/reference/html/","https://roadmap.sh/spring-boot"),
                        p("Hello World REST API","Config-driven multi-profile app")),
                s(r,4,"Building REST APIs","@RestController, DTOs, validation, pagination, error handling.",20,
                        l("https://spring.io/guides/tutorials/rest/","https://www.baeldung.com/rest-with-spring-series"),
                        p("Student grades API","Task tracker with pagination")),
                s(r,5,"Spring Data JPA","Entities, relationships, JPQL, repositories.",22,
                        l("https://www.baeldung.com/spring-data-jpa-tutorial"),
                        p("Blog API with posts + comments","Bookstore API")),
                s(r,6,"Spring Security + JWT","Filter chain, BCrypt, stateless JWT, @PreAuthorize.",18,
                        l("https://www.baeldung.com/spring-security-jwt"),
                        p("Secure auth system","Multi-role API")),
                s(r,7,"Testing","JUnit 5, Mockito, @SpringBootTest, MockMvc.",15,
                        l("https://www.baeldung.com/spring-boot-testing"),
                        p("Unit test all service methods","Integration test for auth flow")),
                s(r,8,"Docker & Deployment","Dockerfile, docker-compose, Render/Railway deployment.",12,
                        l("https://docs.docker.com/guides/java/","https://roadmap.sh/docker"),
                        p("Dockerise Spring Boot + PostgreSQL","Deploy to Render.com free"))
        );
    }

    private void seedFullStackRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("Full Stack Web Development (MERN)").domain("Full Stack")
                .description("React frontend + Node.js/Express backend + MongoDB. From zero to deployed full-stack app. Source: roadmap.sh/full-stack")
                .level(LearningRoadmap.Level.BEGINNER).estimatedWeeks(20).enrolledCount(1540).build());
        steps(r,
                s(r,1,"HTML & CSS","Semantic HTML5, Flexbox, Grid, responsive design.",20,
                        l("https://developer.mozilla.org/en-US/docs/Learn/HTML","https://developer.mozilla.org/en-US/docs/Learn/CSS"),
                        p("Personal portfolio page","Clone a real website homepage")),
                s(r,2,"JavaScript","DOM, events, fetch, async/await, ES6+.",30,l("https://javascript.info"),
                        p("Weather app","Expense tracker with localStorage")),
                s(r,3,"React","Components, hooks, React Router, Context.",28,l("https://react.dev/learn","https://roadmap.sh/react"),
                        p("Movie search app","Shopping cart")),
                s(r,4,"Node.js & Express","REST API, middleware, routing. Source: roadmap.sh/nodejs",22,
                        l("https://nodejs.org/en/learn","https://expressjs.com","https://roadmap.sh/nodejs"),
                        p("Blog REST API","URL shortener")),
                s(r,5,"MongoDB & Mongoose","CRUD, schema design, Atlas free tier.",15,
                        l("https://www.mongodb.com/docs/manual/","https://mongoosejs.com/docs/guide.html"),
                        p("Blog API with MongoDB","E-commerce product API")),
                s(r,6,"Auth — JWT + bcrypt","Password hashing, JWT flow, protected routes.",12,
                        l("https://www.digitalocean.com/community/tutorials/nodejs-jwt-expressjs"),
                        p("Auth system for Blog API","User profile with protected routes")),
                s(r,7,"Connect Frontend + Backend","Axios, CORS, loading states, env vars.",10,
                        l("https://axios-http.com/docs/intro"),
                        p("Full MERN todo app","MERN blog with auth")),
                s(r,8,"Deployment","Vercel (React) + Render (Node) + Atlas (MongoDB) — all free.",10,
                        l("https://vercel.com/docs","https://render.com/docs"),
                        p("Deploy full MERN blog live"))
        );
    }

    private void seedMLRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("AI & Machine Learning with Python").domain("AI/ML")
                .description("From Python basics to deployed ML models. NumPy, Pandas, Scikit-learn, PyTorch, Hugging Face Transformers, FastAPI deployment. Source: roadmap.sh/ai-data-scientist")
                .level(LearningRoadmap.Level.INTERMEDIATE).estimatedWeeks(22).enrolledCount(1090).build());
        steps(r,
                s(r,1,"Python for Data Science","NumPy, Pandas, Matplotlib, EDA workflow. Source: roadmap.sh/python",22,
                        l("https://numpy.org/doc/stable/user/quickstart.html","https://pandas.pydata.org/docs/getting_started/","https://roadmap.sh/python"),
                        p("EDA on Titanic dataset (Kaggle)","Sales data visualisation")),
                s(r,2,"Math for ML","Linear algebra, calculus (gradients), probability, statistics.",15,
                        l("https://www.3blue1brown.com/topics/linear-algebra","https://www.khanacademy.org/math/statistics-probability"),
                        p("Matrix multiplication from scratch","Visualise gradient descent")),
                s(r,3,"Classical ML with Scikit-Learn","Regression, classification, cross-validation, metrics.",28,
                        l("https://scikit-learn.org/stable/user_guide.html","https://www.kaggle.com/learn/intro-to-machine-learning"),
                        p("House price predictor","Spam email classifier")),
                s(r,4,"Deep Learning with PyTorch","Tensors, autograd, CNNs, training loops. Source: roadmap.sh/machine-learning",30,
                        l("https://pytorch.org/tutorials/beginner/basics/intro.html","https://www.fast.ai/"),
                        p("MNIST digit classifier","CIFAR-10 image classifier")),
                s(r,5,"NLP with Hugging Face","Tokenisation, BERT fine-tuning, text classification.",25,
                        l("https://huggingface.co/docs/transformers/index"),
                        p("Movie review sentiment analyser","News topic categoriser")),
                s(r,6,"Model Deployment","FastAPI, Docker, Hugging Face Spaces. Source: roadmap.sh/mlops",15,
                        l("https://fastapi.tiangolo.com/tutorial/","https://huggingface.co/docs/hub/spaces"),
                        p("Deploy sentiment API on HF Spaces","Gradio demo for your model"))
        );
    }

    private void seedDevOpsRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("DevOps & Cloud Engineering").domain("DevOps")
                .description("Linux to Kubernetes: Docker, CI/CD with GitHub Actions, AWS, infrastructure as code. Source: roadmap.sh/devops")
                .level(LearningRoadmap.Level.INTERMEDIATE).estimatedWeeks(20).enrolledCount(640).build());
        steps(r,
                s(r,1,"Linux & Shell Scripting","File system, processes, SSH, bash scripting, cron. Source: roadmap.sh/linux",15,
                        l("https://linuxcommand.org/lc3_learning_the_shell.php","https://roadmap.sh/linux"),
                        p("Automated backup script","Server health monitoring script")),
                s(r,2,"Networking","TCP/IP, DNS, HTTP, firewalls, Nginx reverse proxy.",10,
                        l("https://nginx.org/en/docs/beginners_guide.html"),
                        p("Set up Nginx reverse proxy","Configure UFW firewall")),
                s(r,3,"Docker","Images, Dockerfile, docker-compose, volumes. Source: roadmap.sh/docker",18,
                        l("https://docs.docker.com/get-started/","https://roadmap.sh/docker"),
                        p("Containerise Node.js app","docker-compose: app + PostgreSQL + Nginx")),
                s(r,4,"CI/CD with GitHub Actions","Workflows, secrets, deploy on push. Source: roadmap.sh/github-actions",15,
                        l("https://docs.github.com/en/actions"),
                        p("Auto-run tests on PR","Deploy to Render on merge to main")),
                s(r,5,"AWS Core Services","EC2, S3, RDS, IAM, VPC. Source: roadmap.sh/aws",20,
                        l("https://aws.amazon.com/getting-started/","https://roadmap.sh/aws"),
                        p("Deploy web app on EC2","Host React on S3 + CloudFront")),
                s(r,6,"Kubernetes","Pods, deployments, services, kubectl. Source: roadmap.sh/kubernetes",20,
                        l("https://kubernetes.io/docs/tutorials/kubernetes-basics/","https://roadmap.sh/kubernetes"),
                        p("Deploy on Minikube","Deployment with 3 replicas + LoadBalancer"))
        );
    }

    private void seedAndroidRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("Android Development with Kotlin").domain("Mobile")
                .description("Modern Android: Kotlin, Jetpack Compose, MVVM, Room, Retrofit, Play Store publishing. Source: roadmap.sh/android")
                .level(LearningRoadmap.Level.BEGINNER).estimatedWeeks(18).enrolledCount(720).build());
        steps(r,
                s(r,1,"Kotlin Fundamentals","Variables, null safety, lambdas, coroutines. Source: roadmap.sh/kotlin",20,
                        l("https://kotlinlang.org/docs/getting-started.html","https://play.kotlinlang.org/","https://roadmap.sh/kotlin"),
                        p("Complete Kotlin Koans","CLI expense tracker")),
                s(r,2,"Android Studio Basics","Project structure, Gradle, emulator, Logcat.",12,
                        l("https://developer.android.com/studio","https://developer.android.com/courses/android-basics-compose/course"),
                        p("Hello World on emulator","Simple counter app")),
                s(r,3,"Jetpack Compose UI","Composables, state, LazyColumn, navigation, Material 3.",25,
                        l("https://developer.android.com/jetpack/compose/documentation"),
                        p("Todo list app","Multi-screen app with bottom nav")),
                s(r,4,"MVVM + ViewModel","Architecture, StateFlow, LiveData.",15,
                        l("https://developer.android.com/topic/architecture"),
                        p("Refactor Todo to MVVM","Weather app with ViewModel")),
                s(r,5,"Room Database","Entities, DAOs, TypeConverters, migrations.",14,
                        l("https://developer.android.com/training/data-storage/room"),
                        p("Habit tracker with Room","Offline notes app")),
                s(r,6,"Retrofit Networking","HTTP calls, Gson, OkHttp interceptors.",14,
                        l("https://square.github.io/retrofit/"),
                        p("News reader app","GitHub user search")),
                s(r,7,"Play Store Publishing","Signed AAB, Play Console, testing tracks.",8,
                        l("https://developer.android.com/studio/publish"),
                        p("Publish to internal testing","Set up Firebase Crashlytics"))
        );
    }

    private void seedReactRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("React — Complete Path").domain("Frontend")
                .description("Deep dive into React: hooks, routing, Redux Toolkit, performance, testing. Source: roadmap.sh/react")
                .level(LearningRoadmap.Level.BEGINNER).estimatedWeeks(14).enrolledCount(1820).build());
        steps(r,
                s(r,1,"React Fundamentals","JSX, components, props, conditional rendering, lists, events.",20,
                        l("https://react.dev/learn","https://roadmap.sh/react"),
                        p("Todo list app","Counter with multiple counters")),
                s(r,2,"React Hooks","useState, useEffect, useRef, useCallback, useMemo, custom hooks.",18,
                        l("https://react.dev/reference/react"),
                        p("useFetch custom hook","useLocalStorage hook","Debounced search")),
                s(r,3,"React Router v6","Routes, Link, useNavigate, useParams, protected routes, lazy loading.",10,
                        l("https://reactrouter.com/en/main"),
                        p("Multi-page blog with 404","Protected routes with auth")),
                s(r,4,"Redux Toolkit","createSlice, configureStore, RTK Query, useSelector, useDispatch.",16,
                        l("https://redux-toolkit.js.org/introduction/getting-started"),
                        p("Shopping cart with Redux","Dashboard with RTK Query")),
                s(r,5,"Performance","React.memo, code splitting, Suspense, react-window, Profiler.",10,
                        l("https://react.dev/learn/render-and-commit"),
                        p("Fix render bottleneck with Profiler","Infinite scroll with react-window")),
                s(r,6,"Testing","React Testing Library, Vitest, Playwright E2E.",12,
                        l("https://testing-library.com/docs/react-testing-library/intro/","https://vitest.dev/guide/"),
                        p("Test form component","E2E: search movie → click → see details"))
        );
    }

    private void seedPythonRoadmap() {
        LearningRoadmap r = save(LearningRoadmap.builder().title("Python Programming").domain("Backend")
                .description("Python from fundamentals to FastAPI REST APIs and testing. Source: roadmap.sh/python")
                .level(LearningRoadmap.Level.BEGINNER).estimatedWeeks(12).enrolledCount(2340).build());
        steps(r,
                s(r,1,"Python Basics","Variables, loops, functions, scope, built-in data types.",12,
                        l("https://docs.python.org/3/tutorial/","https://roadmap.sh/python","https://www.learnpython.org/"),
                        p("Number guessing game","Simple calculator CLI")),
                s(r,2,"Data Structures & OOP","Lists, dicts, sets, classes, inheritance, magic methods.",18,
                        l("https://realpython.com/python3-object-oriented-programming/"),
                        p("Student grade book class","Library catalogue with inheritance")),
                s(r,3,"File Handling & Modules","Read/write CSV, os, pathlib, exceptions, your own modules.",8,
                        l("https://docs.python.org/3/tutorial/inputoutput.html"),
                        p("CSV contact reader/writer","Directory organiser by file extension")),
                s(r,4,"Web Scraping & APIs","requests, BeautifulSoup, JSON, public APIs.",12,
                        l("https://docs.python-requests.org/en/latest/"),
                        p("GitHub repo stats fetcher","News headlines scraper")),
                s(r,5,"FastAPI","Path operations, Pydantic, async endpoints, auto OpenAPI docs.",15,
                        l("https://fastapi.tiangolo.com/tutorial/"),
                        p("Todo REST API with FastAPI","Full CRUD with SQLite")),
                s(r,6,"Databases with SQLAlchemy","ORM, models, sessions, relationships.",12,
                        l("https://docs.sqlalchemy.org/en/20/tutorial/"),
                        p("Blog API with SQLAlchemy","FastAPI + PostgreSQL on Neon.tech")),
                s(r,7,"Testing with Pytest","Fixtures, parametrize, mocking, FastAPI TestClient.",8,
                        l("https://docs.pytest.org/en/stable/","https://fastapi.tiangolo.com/tutorial/testing/"),
                        p("Test all CRUD operations","Parametrized edge case tests"))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private LearningRoadmap save(LearningRoadmap r) { return roadmapRepository.save(r); }

    private void steps(LearningRoadmap roadmap, RoadmapStep... steps) {
        for (RoadmapStep s : steps) roadmap.getSteps().add(stepRepository.save(s));
    }

    private RoadmapStep s(LearningRoadmap roadmap, int order, String title, String desc,
                          int hours, List<String> resources, List<String> projects) {
        return RoadmapStep.builder()
                .roadmap(roadmap).title(title).description(desc)
                .stepOrder(order).estimatedHours(hours)
                .resources(new ArrayList<>(resources))
                .projectIdeas(new ArrayList<>(projects))
                .build();
    }

    private List<String> l(String... links)  { return new ArrayList<>(Arrays.asList(links)); }
    private List<String> p(String... projs)  { return new ArrayList<>(Arrays.asList(projs)); }
}
