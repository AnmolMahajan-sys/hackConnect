package com.hackconnect.service;

import com.hackconnect.dto.request.AiMentorRequest;
import com.hackconnect.dto.response.AiMentorResponse;
import com.hackconnect.model.LearningRoadmap;
import com.hackconnect.repository.LearningRoadmapRepository;
import com.hackconnect.service.ai.GeminiClient;
import com.hackconnect.service.ai.GroqClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Mentor Service
 *
 * Strategy:
 *   1. Try Gemini 2.0 Flash first  (15 req/min, 1000 req/day — free)
 *   2. Fall back to Groq Llama 3.3 70B  (30 req/min, 500k tok/day — free)
 *   3. If both down, return a helpful rule-based answer (always works)
 *
 * All answers are grounded with REAL data from your database:
 *   - The system prompt includes actual roadmap titles, domains, and step counts
 *   - This prevents the AI from hallucinating roadmap names that don't exist
 *   - Opportunity suggestions come from the DB, not the AI's imagination
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiMentorService {

    private final GeminiClient              geminiClient;
    private final GroqClient                groqClient;
    private final LearningRoadmapRepository roadmapRepository;

    // ── Domain keyword detection ─────────────────────────────────────────────
    private static final Map<String, List<String>> DOMAIN_KEYWORDS = Map.of(
        "Full Stack", List.of("full stack", "fullstack", "mern", "mean", "full-stack", "web app"),
        "Frontend",   List.of("frontend", "front-end", "react", "vue", "angular", "html", "css", "ui", "ux"),
        "Backend",    List.of("backend", "back-end", "spring", "node", "api", "server", "java", "python server"),
        "AI/ML",      List.of("machine learning", "deep learning", "ml", "ai", "neural", "tensorflow", "pytorch", "data science"),
        "Mobile",     List.of("mobile", "android", "ios", "flutter", "react native", "app")
    );

    // ── Main entry point ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AiMentorResponse chat(AiMentorRequest request) {
        String question      = request.getMessage();
        String detectedDomain = detectDomain(question.toLowerCase());

        // Fetch relevant roadmaps from DB — real data, not AI hallucinations
        List<LearningRoadmap> relevantRoadmaps = fetchRelevantRoadmaps(detectedDomain);

        // Build a system prompt that INCLUDES your actual DB content
        // This is the key to correct, grounded answers
        String systemPrompt = buildSystemPrompt(relevantRoadmaps);

        // Try providers in order: Gemini → Groq → rule-based fallback
        String reply;
        String provider;

        if (geminiClient.isConfigured()) {
            try {
                reply    = geminiClient.chat(systemPrompt, question, request.getHistory());
                provider = "gemini";
                log.debug("Answered via Gemini");
            } catch (Exception e) {
                log.warn("Gemini failed, trying Groq...");
                reply    = tryGroqOrFallback(systemPrompt, question, request);
                provider = groqClient.isConfigured() ? "groq" : "fallback";
            }
        } else if (groqClient.isConfigured()) {
            try {
                reply    = groqClient.chat(systemPrompt, question, request.getHistory());
                provider = "groq";
                log.debug("Answered via Groq");
            } catch (Exception e) {
                log.warn("Groq failed, using rule-based fallback...");
                reply    = ruleBasedReply(question, detectedDomain, relevantRoadmaps);
                provider = "fallback";
            }
        } else {
            log.info("No AI provider configured — using rule-based replies");
            reply    = ruleBasedReply(question, detectedDomain, relevantRoadmaps);
            provider = "fallback";
        }

        List<AiMentorResponse.RoadmapSuggestion> suggestions = relevantRoadmaps.stream()
            .map(r -> AiMentorResponse.RoadmapSuggestion.builder()
                .id(r.getId())
                .title(r.getTitle())
                .domain(r.getDomain())
                .level(r.getLevel())
                .estimatedWeeks(r.getEstimatedWeeks())
                .enrolledCount(r.getEnrolledCount())
                .build())
            .collect(Collectors.toList());

        return AiMentorResponse.builder()
            .reply(reply)
            .provider(provider)
            .suggestedRoadmaps(suggestions)
            .detectedDomain(detectedDomain)
            .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String tryGroqOrFallback(String systemPrompt, String question, AiMentorRequest req) {
        if (!groqClient.isConfigured()) {
            String domain = detectDomain(question.toLowerCase());
            List<LearningRoadmap> maps = fetchRelevantRoadmaps(domain);
            return ruleBasedReply(question, domain, maps);
        }
        try {
            return groqClient.chat(systemPrompt, question, req.getHistory());
        } catch (Exception e) {
            String domain = detectDomain(question.toLowerCase());
            return ruleBasedReply(question, domain, fetchRelevantRoadmaps(domain));
        }
    }

    /**
     * Builds a system prompt that INJECTS your real database content.
     * This is what makes the AI answers correct and specific to HackConnect —
     * the AI knows exactly which roadmaps exist, how long they take, etc.
     */
    private String buildSystemPrompt(List<LearningRoadmap> roadmaps) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            You are HackConnect's AI Mentor — a friendly, knowledgeable guide for college students
            in India and worldwide who want to grow their technical skills and find opportunities.

            Your goals:
            1. Give SPECIFIC, ACTIONABLE advice — not generic platitudes.
            2. Recommend learning resources that actually exist and are free (MDN, freeCodeCamp,
               official docs, YouTube channels like Traversy Media, Fireship, etc.).
            3. Suggest beginner-friendly projects the student can build this week.
            4. Point to the roadmaps listed below — these are the ONLY roadmaps on HackConnect,
               so only recommend from this list.
            5. Be encouraging but honest. If a student asks about something outside tech,
               gently redirect to learning and career growth.
            6. Keep responses under 300 words. Use bullet points where helpful.
            7. End every response with one specific "Next step" the student can do TODAY.

            AVAILABLE ROADMAPS ON HACKCONNECT:
            """);

        if (roadmaps.isEmpty()) {
            sb.append("(No roadmaps found in database)\n");
        } else {
            for (LearningRoadmap r : roadmaps) {
                sb.append(String.format("  • %s | Domain: %s | Level: %s | %d weeks | %d students enrolled\n",
                    r.getTitle(), r.getDomain(), r.getLevel(), r.getEstimatedWeeks(), r.getEnrolledCount()));
            }
        }

        sb.append("""

            TONE: Friendly, motivating, direct. Talk like a senior student helping a junior,
            not like a corporate chatbot. Use "you" not "one". Never say "Great question!".
            Never make up roadmap names not in the list above.
            """);

        return sb.toString();
    }

    /**
     * Rule-based fallback — works with zero API keys.
     * Returns genuinely useful answers for common questions.
     */
    private String ruleBasedReply(String question,
                                   String detectedDomain,
                                   List<LearningRoadmap> roadmaps) {
        String q = question.toLowerCase();

        // Detect intent
        if (q.contains("start") || q.contains("begin") || q.contains("new") || q.contains("beginner")) {
            return buildBeginnerReply(detectedDomain, roadmaps);
        }
        if (q.contains("project") || q.contains("build") || q.contains("idea")) {
            return buildProjectReply(detectedDomain);
        }
        if (q.contains("job") || q.contains("placement") || q.contains("internship") || q.contains("career")) {
            return buildCareerReply(detectedDomain);
        }
        if (q.contains("hackathon") || q.contains("competition") || q.contains("contest")) {
            return "For hackathons, check the Opportunities section on HackConnect! " +
                   "Filter by type=HACKATHON to see upcoming ones. " +
                   "Pro tip: Start with online hackathons (filter online=true) — " +
                   "no travel needed and you can participate from college.\n\n" +
                   "**Next step:** Visit /opportunities?type=HACKATHON&upcomingOnly=true right now.";
        }
        if (q.contains("roadmap") || q.contains("path") || q.contains("learn")) {
            return buildRoadmapReply(detectedDomain, roadmaps);
        }

        // Generic helpful reply
        return buildBeginnerReply(detectedDomain, roadmaps);
    }

    private String buildBeginnerReply(String domain, List<LearningRoadmap> roadmaps) {
        if (domain == null) {
            return """
                Great place to start! Here's what I'd suggest:

                **If you like building websites** → Start with the Full Stack roadmap. HTML + CSS first, then JavaScript, then React.

                **If you're into data/AI** → Start with the AI/ML roadmap. Python is the first step and it takes about 2 weeks.

                **If you want backend/servers** → Spring Boot roadmap (Java). Great for placement prep.

                All these roadmaps are free on HackConnect. Each step has curated resources and project ideas.

                **Next step:** Go to /roadmaps and click "Enroll" on whichever domain excites you most.
                """;
        }

        String roadmapList = roadmaps.stream()
            .map(r -> "  • " + r.getTitle() + " (" + r.getEstimatedWeeks() + " weeks)")
            .collect(Collectors.joining("\n"));

        return "For **" + domain + "**, here's your path:\n\n" +
               "Available roadmaps:\n" + roadmapList + "\n\n" +
               "Start with the BEGINNER level roadmap. Each step has free resources and project ideas. " +
               "Even 1 hour a day will get you through it.\n\n" +
               "**Next step:** Enroll in the beginner " + domain + " roadmap right now — it takes 30 seconds.";
    }

    private String buildProjectReply(String domain) {
        Map<String, String> projects = Map.of(
            "Full Stack", "Build a **Job Board app**: users post jobs, others apply. Uses React frontend + Node.js API + MongoDB. Covers auth, CRUD, file upload. Takes 2 weekends.",
            "Frontend",   "Build a **GitHub Profile Finder**: search any GitHub username, show their repos and stats using the GitHub API. Pure HTML/CSS/JS — no backend needed. Takes 1 weekend.",
            "Backend",    "Build a **URL Shortener API** (like bit.ly): POST a long URL, get a short code back, redirect on GET. Spring Boot + H2 database. Deploy free on Render.com.",
            "AI/ML",      "Build a **Movie Sentiment Analyser**: input a movie review, get positive/negative/neutral. Use Hugging Face's free hosted API — no GPU needed. Takes 1 day.",
            "Mobile",     "Build a **Habit Tracker app**: add habits, mark them daily, see a streak counter. Flutter or React Native. Pure offline app, no backend. Takes 1 weekend."
        );

        String project = domain != null ? projects.getOrDefault(domain,
            "Build a **To-Do List app with user auth** — simple, covers HTML/CSS/JS/backend basics, and every recruiter recognises it as a fundamentals check.") :
            "Build a **To-Do List app with user auth** — covers frontend + backend + database. Every recruiter recognises it as a fundamentals check.";

        return project + "\n\n**Next step:** Open VS Code right now and create a new folder for this project.";
    }

    private String buildCareerReply(String domain) {
        return """
            For placement prep in tech, here's what actually matters in 2024–25:

            **Resume**: 2-3 good projects beats 10 mediocre ones. Host everything on GitHub.

            **Skills that get interviews**:
            • Data Structures & Algorithms (LeetCode — do 100 easy/medium problems)
            • One full-stack project deployed live (Vercel + Render are free)
            • System Design basics (for senior roles/off-campus)

            **For internships**:
            • GSoC (Google Summer of Code) — check HackConnect Opportunities
            • Microsoft Engage, Amazon ML Summer School
            • Apply via LinkedIn, Internshala, Unstop

            **Timeline**: Start DSA now. Build a project this month. Apply 3 months before semester ends.

            **Next step:** Solve 1 LeetCode easy problem today. Seriously — just one.
            """;
    }

    private String buildRoadmapReply(String domain, List<LearningRoadmap> roadmaps) {
        if (roadmaps.isEmpty()) {
            return "Check the Roadmaps section on HackConnect — we have curated paths for " +
                   "Full Stack, Frontend, Backend, AI/ML, and more. Each has free resources and project ideas.";
        }
        String list = roadmaps.stream()
            .map(r -> String.format("• **%s** — %s level, %d weeks, %d students enrolled",
                r.getTitle(), r.getLevel(), r.getEstimatedWeeks(), r.getEnrolledCount()))
            .collect(Collectors.joining("\n"));
        return "Here are the roadmaps available" +
               (domain != null ? " for **" + domain + "**" : "") + ":\n\n" + list +
               "\n\n**Next step:** Click on the one that matches your goal and enroll — it's free.";
    }

    private String detectDomain(String text) {
        for (Map.Entry<String, List<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (text.contains(kw)) return entry.getKey();
            }
        }
        return null;
    }

    private List<LearningRoadmap> fetchRelevantRoadmaps(String domain) {
        if (domain != null) {
            List<LearningRoadmap> found = roadmapRepository.findByDomainIgnoreCase(domain);
            if (!found.isEmpty()) return found;
        }
        // Return all roadmaps so the AI can recommend across domains
        return roadmapRepository.findAll();
    }
}
