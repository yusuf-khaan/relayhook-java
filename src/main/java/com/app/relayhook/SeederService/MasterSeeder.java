package com.app.relayhook.SeederService;

import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Repository.SystemIntegrationsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MasterSeeder implements CommandLineRunner {

    @Autowired
    private SystemIntegrationsRepository systemIntegrationsRepository;

    @Override
    public void run(String... args) {
        for (String arg : args) {

            if (arg.equalsIgnoreCase("seed=systemIntegration")) {
                seedSystemIntegrations();
            }

            if (arg.equalsIgnoreCase("seed=products")) {
                seedDraftWorkflows();
            }

            if (arg.equalsIgnoreCase("seed=all")) {
                seedAll();
            }
        }
    }

    private void seedSystemIntegrations() {
        System.out.println(">>> RUNNING SystemIntegrations SEEDER <<<");

        List<SystemIntegrations> integrations = new ArrayList<>();

        integrations.add(new SystemIntegrations(
                null,
                "gmail",
                true,
                "https://upload.wikimedia.org/wikipedia/commons/4/4e/Gmail_Icon.png",
                Arrays.asList("email", "google"),
                Map.of(
                        "scopes", "https://mail.google.com/",
                        "client_id", "",
                        "client_secret", ""),
                "Gmail email integration for sending and reading mail.",
                "OAuth2",
                "v1",
                "https://www.googleapis.com/gmail/v1",
                "gmail",
                "https://yourdomain.com/api/integrations/gmail/callback",
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "twilio",
                true,
                "https://imgs.search.brave.com/LNYqI143S-RS76yyeNWo0RL2b-g_XjQmBzDv2zDVDOg/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9pbWcu/aWNvbnM4LmNvbS9l/eHRlcm5hbC10YWwt/cmV2aXZvLXNoYWRv/dy10YWwtcmV2aXZv/LzEyMDAvZXh0ZXJu/YWwtdHdpbGlvLWlz/LWEtY2xvdWQtY29t/bXVuaWNhdGlvbnMt/cGxhdGZvcm0tYXMt/YS1zZXJ2aWNlLWNv/bXBhbnktbG9nby1z/aGFkb3ctdGFsLXJl/dml2by5qcGc",
                Arrays.asList("sms", "call"),
                Map.of(
                        "accountSid", "",
                        "authToken", ""),
                "Twilio Integration for sms/call",
                "token",
                "v1",
                "https://twilio.com",
                "twilio",
                "https://yourdomain.com/api/integrations/gmail/callback",
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "postgres",
                true,
                "https://imgs.search.brave.com/86NRwygvRbvn3nZj5l7Ob62niTPFPgx2f_nvVru6QyY/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9jZG4u/aWNvbnNjb3V0LmNv/bS9pY29uL2ZyZWUv/cG5nLTI1Ni9mcmVl/LXBvc3RncmVzcWwt/aWNvbi1zdmctZG93/bmxvYWQtcG5nLTEx/NzUxMjAucG5nP2Y9/d2VicCZ3PTI1Ng",
                Arrays.asList(""),
                Map.of(
                        "host", "",
                        "port", 0,
                        "database", "",
                        "user", "",
                        "password", "",
                        "ssl", false),
                "Postgres.",
                "tokenized",
                "v1",
                "https://postgres.com/",
                "postgres",
                null,
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "openai",
                true,
                "https://imgs.search.brave.com/Bfi1b826AMn-8-jGFIcjMlq-kct3_ynA_8AH0bpx5o0/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9zdGF0/aWMudmVjdGVlenku/Y29tL3N5c3RlbS9y/ZXNvdXJjZXMvdGh1/bWJuYWlscy8wMjQv/NTU4LzgwNS9zbWFs/bC9vcGVuYWktY2hh/dGdwdC1sb2dvLWlj/b24tZnJlZS1wbmcu/cG5n",
                Arrays.asList(""),
                Map.of(
                        "api_key", null),
                "open ai.",
                "tokenized",
                "v1",
                "https://openai.com/",
                "openai",
                null,
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "instagram",
                true,
                "https://upload.wikimedia.org/wikipedia/commons/e/e7/Instagram_logo_2016.svg",
                Arrays.asList("social", "facebook-meta"),
                Map.of(
                        "scopes", "basic,media",
                        "access_token", ""),
                "Instagram API integration for post insights and publishing.",
                "OAuth2",
                "v1",
                "https://graph.instagram.com/",
                "instagram",
                "https://yourdomain.com/api/integrations/instagram/callback",
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "x",
                true,
                "https://upload.wikimedia.org/wikipedia/commons/5/53/X_logo_2023.svg",
                Arrays.asList("social"),
                Map.of(
                        "scopes", "tweet.read,tweet.write",
                        "consumerKey", "",
                        "consumerSecret", "",
                        "accessToken", "",
                        "accessTokenSecret", ""),
                "X (Twitter) integration for social posting and analytics.",
                "OAuth2",
                "v2",
                "https://api.twitter.com/",
                "x",
                "https://yourdomain.com/api/integrations/x/callback",
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "Sandbox (JS Compiler)",
                true,
                "https://raw.githubusercontent.com/github/explore/main/topics/javascript/javascript.png",
                Arrays.asList("developer-tools"),
                Map.of(
                        "api_key", ""),
                "JavaScript code compilation and sandbox execution API.",
                "none",
                "v1",
                "",
                "sandbox",
                "",
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "sheets",
                true,
                "https://upload.wikimedia.org/wikipedia/commons/3/3f/Google_Sheets_logo.svg",
                Arrays.asList("google", "spreadsheet"),
                Map.of(
                        "scopes", "https://www.googleapis.com/auth/spreadsheets",
                        "client_id", "",
                        "client_secret", ""),
                "Google Sheets API for reading and writing spreadsheets.",
                "OAuth2",
                "v4",
                "https://sheets.googleapis.com/v4",
                "sheets",
                "https://yourdomain.com/api/integrations/sheets/callback",
                null,
                null));

        integrations.add(new SystemIntegrations(
                null,
                "jira",
                true,
                "https://upload.wikimedia.org/wikipedia/commons/8/8b/Atlassian_logo.svg",
                Arrays.asList("project-management", "atlassian"),
                Map.of(
                        "scopes", "read:jira-work write:jira-work offline_access",
                        "base_url", "",
                        "email", "",
                        "accessToken", ""),
                "Jira integration for project issue tracking, boards, and workflow automation.",
                "OAuth2",
                "v3",
                "https://api.atlassian.com/ex/jira/",
                "jira",
                "https://yourdomain.com/api/integrations/jira/callback",
                null,
                null));

        for (SystemIntegrations integration : integrations) {
            if (!systemIntegrationsRepository.existsByProvider(integration.getProvider())) {
                systemIntegrationsRepository.save(integration);
            } else {
                System.out.println("Skipped (already exists): " + integration.getProvider());
            }
        }
        System.out.println(">>> SystemIntegrations SEEDING DONE <<<");
        System.exit(0);
    }

    private void seedDraftWorkflows() {
        System.out.println(">>> RUNNING workflow SEEDER <<<");
    }

    private void seedAll() {
        seedSystemIntegrations();
        seedDraftWorkflows();
    }
}
