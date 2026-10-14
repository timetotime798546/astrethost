package com.resumeai.app;

import java.util.ArrayList;
import java.util.List;

public class ResumeGeneratorEngine {

    public static class CareerPackage {
        private final String resumeText;
        private final String coverLetterText;

        public CareerPackage(String resumeText, String coverLetterText) {
            this.resumeText = resumeText;
            this.coverLetterText = coverLetterText;
        }

        public String getResumeText() {
            return resumeText;
        }

        public String getCoverLetterText() {
            return coverLetterText;
        }
    }

    public static CareerPackage generate(String rawInput) {
        String trimmed = (rawInput == null) ? "" : rawInput.trim();
        String jobTitle = "Professional Specialist";
        List<String> skillsList = new ArrayList<String>();

        if (!trimmed.isEmpty()) {
            String[] segments = trimmed.split("[,;\\n]+");
            if (segments.length > 0 && segments[0].trim().length() > 0) {
                jobTitle = segments[0].trim();
            }

            for (int i = 1; i < segments.length; i++) {
                String s = segments[i].trim();
                if (!s.isEmpty()) {
                    skillsList.add(s);
                }
            }
        }

        if (skillsList.isEmpty()) {
            skillsList.add("Strategic Planning");
            skillsList.add("Team Collaboration");
            skillsList.add("Problem Solving");
            skillsList.add("Performance Optimization");
            skillsList.add("Process Automation");
        }

        String primarySkillStr = joinSkills(skillsList, " • ");

        // Build Resume
        StringBuilder rb = new StringBuilder();
        rb.append("====================================================\n");
        rb.append("ALEX MORGAN\n");
        rb.append("alex.morgan@email.com | +1 (555) 019-2834 | San Francisco, CA\n");
        rb.append("LinkedIn: linkedin.com/in/alexmorgan | GitHub: github.com/alexmorgan\n");
        rb.append("====================================================\n\n");

        rb.append("TARGET POSITION: ").append(jobTitle.toUpperCase()).append("\n\n");

        rb.append("--- PROFESSIONAL SUMMARY ---\n");
        rb.append("Results-driven ").append(jobTitle).append(" with a documented record of delivering ")
          .append("scalable, high-performance solutions. Adept at leveraging modern methodologies and industry ")
          .append("best practices including ").append(joinSkills(skillsList, ", ")).append(". Proven capacity ")
          .append("to collaborate cross-functionally, accelerate time-to-market by up to 35%, and maintain high quality standards.\n\n");

        rb.append("--- CORE COMPETENCIES & TECHNICAL SKILLS ---\n");
        rb.append("• Proficiencies: ").append(primarySkillStr).append("\n");
        rb.append("• Methodologies: Agile/Scrum, CI/CD, Test-Driven Development, Root Cause Analysis\n");
        rb.append("• Tooling & Platforms: Git, Docker, Cloud Infrastructures, Analytics & Performance Profiling\n\n");

        rb.append("--- PROFESSIONAL EXPERIENCE ---\n");
        rb.append("Lead ").append(jobTitle).append(" | Apex Innovations Corp\n");
        rb.append("Jan 2022 – Present | Austin, TX\n");
        rb.append("• Architected and executed critical initiatives utilizing ").append(skillsList.get(0))
          .append(", enhancing client platform throughput by 42%.\n");
        if (skillsList.size() > 1) {
            rb.append("• Standardized implementation workflows around ").append(skillsList.get(1))
              .append(", reducing technical debt and onboarding turnaround by 28%.\n");
        }
        rb.append("• Mentored an engineering squad of 6 contributors, instituting code review standards and unit testing.\n");
        rb.append("• Spearheaded automated performance audits that minimized system latencies by 250ms.\n\n");

        rb.append(jobTitle).append(" | Horizon Technologies\n");
        rb.append("Jul 2019 – Dec 2021 | San Francisco, CA\n");
        if (skillsList.size() > 2) {
            rb.append("• Applied ").append(skillsList.get(2)).append(" to revamp key enterprise customer journeys and data pipelines.\n");
        } else {
            rb.append("• Delivered enterprise software modules handling over 500,000 monthly active user interactions.\n");
        }
        rb.append("• Partnered with Product and Design teams to translate user pain points into production-grade solutions.\n");
        rb.append("• Received the Quarterly Excellence Award for leading delivery 3 weeks ahead of scheduled deadlines.\n\n");

        rb.append("--- KEY PROJECTS ---\n");
        rb.append("• NextGen Platform Revamp: Built a modular architecture featuring ")
          .append(skillsList.get(0)).append("; reached 99.98% reliability.\n");
        rb.append("• Open-Source Contributor: Maintained developer tools leveraged by 12,000+ engineers globally.\n\n");

        rb.append("--- EDUCATION & CERTIFICATIONS ---\n");
        rb.append("• Bachelor of Science in Computer Science / Applied Engineering\n");
        rb.append("  State University of Technology (Honors Graduate)\n");
        rb.append("• Professional Industry Certification: Advanced ").append(jobTitle).append(" Specialization\n");

        // Build Cover Letter
        StringBuilder cb = new StringBuilder();
        cb.append("Alex Morgan\n");
        cb.append("alex.morgan@email.com | +1 (555) 019-2834\n");
        cb.append("San Francisco, CA\n\n");
        cb.append("Date: Current Date\n\n");
        cb.append("Hiring Team / Senior Leadership\n");
        cb.append("Target Organization\n\n");
        cb.append("RE: Application for ").append(jobTitle).append("\n\n");
        cb.append("Dear Hiring Manager,\n\n");
        cb.append("I am writing to express my eager interest in the ").append(jobTitle)
          .append(" opening at your organization. Having closely followed your team's impactful milestones, ")
          .append("I am confident that my practical background in ").append(primarySkillStr)
          .append(" positions me to contribute meaningfully from day one.\n\n");

        cb.append("In my previous responsibilities as Lead ").append(jobTitle)
          .append(", I spearheaded initiatives that optimized performance metrics by 42% and introduced streamlined ")
          .append("frameworks. By integrating ").append(skillsList.get(0))
          .append(" with scalable engineering principles, I solved mission-critical bottlenecks and ensured transparent ")
          .append("collaboration across departments.\n\n");

        cb.append("What excites me most about your team is the dedication to product excellence and rapid innovation. ")
          .append("I thrive in environments where ownership, continuous learning, and clean architectural execution ")
          .append("are valued. I welcome the opportunity to bring my hands-on background in ")
          .append(joinSkills(skillsList, ", ")).append(" to help accelerate your organizational targets.\n\n");

        cb.append("Thank you for your time and consideration. I look forward to the possibility of discussing how ")
          .append("my capabilities align with your forward-looking roadmap.\n\n");
        cb.append("Warm regards,\n\n");
        cb.append("Alex Morgan\n");

        return new CareerPackage(rb.toString(), cb.toString());
    }

    private static String joinSkills(List<String> list, String delimiter) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            b.append(list.get(i));
            if (i < list.size() - 1) {
                b.append(delimiter);
            }
        }
        return b.toString();
    }
}