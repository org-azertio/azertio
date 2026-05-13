package org.azertio.plugins.pdfreport.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.contributors.ReportBuilder;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfOutputTest {

    private static final Path OUTPUT_DIR = Path.of("target/test-output/pdf");

    @Test
    void generatesSamplePdfReport() throws Exception {
        java.nio.file.Files.createDirectories(OUTPUT_DIR);

        Path logoPath = OUTPUT_DIR.resolve("test-logo.png");
        generateTestLogo(logoPath);

        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources",
            "core.resourceFilter", "**/*.feature",
            "pdfreport.outputDir", OUTPUT_DIR.toAbsolutePath().toString(),
            "pdfreport.title",    "Sample REST API Report",
            "pdfreport.includePassedSteps", "true",
            "pdfreport.pageBreak", "suite",
            "pdfreport.logoPath", logoPath.toAbsolutePath().toString(),
            "pdfreport.accentColor", "#1A4A6A",
            "pdfreport.footer", "Confidential - ACME Corporation - Internal use only"
        ));
        AzertioRuntime runtime = new AzertioRuntime(config);

        TestPlanRepository      planRepo = runtime.getRepository(TestPlanRepository.class);
        TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

        // ---- build plan tree ----

        UUID projectID = planRepo.persistProject(
            new TestProject("REST API Suite", "Sample Azertio project", "Acme Corp", List.of()));

        UUID root = planRepo.persistNode(
            new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("REST API Suite"));

        // ── Suite 1: Authentication ──────────────────────────────────────────
        UUID suiteAuth = child(planRepo, root, NodeType.TEST_SUITE, null, "Authentication");

        UUID featLogin = child(planRepo, suiteAuth, NodeType.TEST_FEATURE, "Feature", "Login API");

        UUID tcLogin = scenario(planRepo, featLogin, "Successful login returns a JWT token", "AUTH-001", "smoke", "auth");
        UUID sL1 = step(planRepo, tcLogin, "Given", "the REST API is running");
        UUID sL2 = stepWithDoc(planRepo, tcLogin, "When", "I send POST /auth/login with valid credentials",
            "application/json", "{\n  \"username\": \"admin\",\n  \"password\": \"secret123\"\n}");
        UUID sL3 = step(planRepo, tcLogin, "Then", "the response status code is 200");
        UUID sL4 = step(planRepo, tcLogin, "And",  "the response body contains a 'token' field");

        UUID tcBadPwd = scenario(planRepo, featLogin, "Login with wrong password is rejected", "AUTH-002", "auth", "negative");
        UUID sB1 = step(planRepo, tcBadPwd, "Given", "the REST API is running");
        UUID sB2 = step(planRepo, tcBadPwd, "When",  "I send POST /auth/login with wrong password");
        UUID sB3 = step(planRepo, tcBadPwd, "Then",  "the response status code is 401");
        UUID sB4 = step(planRepo, tcBadPwd, "And",   "the error message is 'Invalid credentials'");

        UUID tcLocked = scenario(planRepo, featLogin, "Login with a locked account returns 403", "AUTH-003", "auth", "negative");
        UUID sK1 = step(planRepo, tcLocked, "Given", "the REST API is running");
        UUID sK2 = step(planRepo, tcLocked, "And",   "the user account 'blocked@example.com' is locked");
        UUID sK3 = step(planRepo, tcLocked, "When",  "I send POST /auth/login with that account");
        UUID sK4 = step(planRepo, tcLocked, "Then",  "the response status code is 403");
        UUID sK5 = step(planRepo, tcLocked, "And",   "the error message is 'Account locked'");

        UUID featRefresh = child(planRepo, suiteAuth, NodeType.TEST_FEATURE, "Feature", "Token Refresh");

        UUID tcRefresh = scenario(planRepo, featRefresh, "Refresh an expired token and receive a new one", "AUTH-004", "auth");
        UUID sR1 = step(planRepo, tcRefresh, "Given", "I have an expired JWT token");
        UUID sR2 = step(planRepo, tcRefresh, "When",  "I send POST /auth/refresh");
        UUID sR3 = step(planRepo, tcRefresh, "Then",  "the response status code is 200");
        UUID sR4 = step(planRepo, tcRefresh, "And",   "I receive a new valid token in the response body");

        UUID tcRefreshInvalid = scenario(planRepo, featRefresh, "Refresh with a forged token is rejected", "AUTH-005", "auth", "security");
        UUID sRI1 = step(planRepo, tcRefreshInvalid, "Given", "I have a token signed with an unknown secret");
        UUID sRI2 = step(planRepo, tcRefreshInvalid, "When",  "I send POST /auth/refresh with that token");
        UUID sRI3 = step(planRepo, tcRefreshInvalid, "Then",  "the response status code is 401");

        UUID featLogout = child(planRepo, suiteAuth, NodeType.TEST_FEATURE, "Feature", "Logout");

        UUID tcLogout = scenario(planRepo, featLogout, "Logout invalidates the session token", "AUTH-006", "auth", "smoke");
        UUID sO1 = step(planRepo, tcLogout, "Given", "I am authenticated with a valid JWT token");
        UUID sO2 = step(planRepo, tcLogout, "When",  "I send POST /auth/logout");
        UUID sO3 = step(planRepo, tcLogout, "Then",  "the response status code is 204");
        UUID sO4 = step(planRepo, tcLogout, "And",   "subsequent requests with the same token return 401");

        // ── Suite 2: User Management ─────────────────────────────────────────
        UUID suiteUsers = child(planRepo, root, NodeType.TEST_SUITE, null, "User Management");

        UUID featUserCrud = child(planRepo, suiteUsers, NodeType.TEST_FEATURE, "Feature", "User CRUD Operations");

        UUID tcCreate = scenario(planRepo, featUserCrud, "Create a new user with valid payload", "USR-001", "crud", "smoke");
        UUID sC1 = step(planRepo, tcCreate, "Given", "I am authenticated as admin");
        UUID sC2 = stepWithTable(planRepo, tcCreate, "When", "I send POST /users with a valid payload",
            List.of(
                List.of("field",    "value"),
                List.of("username", "john.doe"),
                List.of("email",    "john.doe@example.com"),
                List.of("role",     "viewer")
            ));
        UUID sC3 = step(planRepo, tcCreate, "Then", "the response status code is 201");
        UUID sC4 = step(planRepo, tcCreate, "And",  "the new user appears in GET /users");

        UUID tcUpdate = scenario(planRepo, featUserCrud, "Update an existing user's email address", "USR-002", "crud");
        UUID sU1 = step(planRepo, tcUpdate, "Given", "I am authenticated as admin");
        UUID sU2 = step(planRepo, tcUpdate, "And",   "a user with username 'john.doe' exists");
        UUID sU3 = step(planRepo, tcUpdate, "When",  "I send PATCH /users/john.doe with a new email");
        UUID sU4 = step(planRepo, tcUpdate, "Then",  "the response status code is 200");
        UUID sU5 = step(planRepo, tcUpdate, "And",   "GET /users/john.doe returns the updated email");

        UUID tcDelete = scenario(planRepo, featUserCrud,
            "Delete a non-existent user triggers a 404 response", "USR-003", "crud", "negative");
        UUID sD1 = step(planRepo, tcDelete, "Given", "I am authenticated as admin");
        UUID sD2 = step(planRepo, tcDelete, "When",  "I send DELETE /users/9999");
        UUID sD3 = step(planRepo, tcDelete, "Then",  "the response status code is 404");

        UUID featRoles = child(planRepo, suiteUsers, NodeType.TEST_FEATURE, "Feature", "Role Management");

        UUID tcPromote = scenario(planRepo, featRoles, "Promote a viewer to admin role", "USR-004", "roles", "smoke");
        UUID sP1 = step(planRepo, tcPromote, "Given", "I am authenticated as super-admin");
        UUID sP2 = step(planRepo, tcPromote, "And",   "a user 'john.doe' with role 'viewer' exists");
        UUID sP3 = step(planRepo, tcPromote, "When",  "I send PUT /users/john.doe/role with body {\"role\":\"admin\"}");
        UUID sP4 = step(planRepo, tcPromote, "Then",  "the response status code is 200");
        UUID sP5 = step(planRepo, tcPromote, "And",   "GET /users/john.doe returns role 'admin'");

        UUID tcSelfPromote = scenario(planRepo, featRoles, "A non-admin user cannot promote themselves", "USR-005", "roles", "security", "negative");
        UUID sSP1 = step(planRepo, tcSelfPromote, "Given", "I am authenticated as a regular viewer");
        UUID sSP2 = step(planRepo, tcSelfPromote, "When",  "I send PUT /users/me/role with body {\"role\":\"admin\"}");
        UUID sSP3 = step(planRepo, tcSelfPromote, "Then",  "the response status code is 403");

        // ── Suite 3: Order Management ────────────────────────────────────────
        UUID suiteOrders = child(planRepo, root, NodeType.TEST_SUITE, null, "Order Management");

        UUID featOrderPlacement = child(planRepo, suiteOrders, NodeType.TEST_FEATURE, "Feature", "Order Placement");

        UUID tcPlaceOrder = scenario(planRepo, featOrderPlacement, "Place a standard order with in-stock items", "ORD-001", "orders", "smoke");
        UUID sPO1 = step(planRepo, tcPlaceOrder, "Given", "I am authenticated as a customer");
        UUID sPO2 = step(planRepo, tcPlaceOrder, "And",   "product SKU-100 has 10 units in stock");
        UUID sPO3 = stepWithTable(planRepo, tcPlaceOrder, "When", "I send POST /orders with",
            List.of(
                List.of("sku",     "qty"),
                List.of("SKU-100", "2"),
                List.of("SKU-201", "1")
            ));
        UUID sPO4 = step(planRepo, tcPlaceOrder, "Then", "the response status code is 201");
        UUID sPO5 = step(planRepo, tcPlaceOrder, "And",  "the order status is 'PENDING'");
        UUID sPO6 = step(planRepo, tcPlaceOrder, "And",  "an order confirmation email is sent");

        UUID tcOutOfStock = scenario(planRepo, featOrderPlacement, "Order is rejected when item is out of stock", "ORD-002", "orders", "negative");
        UUID sOS1 = step(planRepo, tcOutOfStock, "Given", "I am authenticated as a customer");
        UUID sOS2 = step(planRepo, tcOutOfStock, "And",   "product SKU-999 has 0 units in stock");
        UUID sOS3 = step(planRepo, tcOutOfStock, "When",  "I send POST /orders with SKU-999 qty 1");
        UUID sOS4 = step(planRepo, tcOutOfStock, "Then",  "the response status code is 422");
        UUID sOS5 = step(planRepo, tcOutOfStock, "And",   "the error message mentions 'out of stock'");

        UUID tcCancelOrder = scenario(planRepo, featOrderPlacement, "Cancel a pending order", "ORD-003", "orders");
        UUID sCO1 = step(planRepo, tcCancelOrder, "Given", "I am authenticated as a customer");
        UUID sCO2 = step(planRepo, tcCancelOrder, "And",   "I have a pending order with ID 42");
        UUID sCO3 = step(planRepo, tcCancelOrder, "When",  "I send DELETE /orders/42");
        UUID sCO4 = step(planRepo, tcCancelOrder, "Then",  "the response status code is 200");
        UUID sCO5 = step(planRepo, tcCancelOrder, "And",   "the order status changes to 'CANCELLED'");

        UUID featOrderHistory = child(planRepo, suiteOrders, NodeType.TEST_FEATURE, "Feature", "Order History");

        UUID tcListOrders = scenario(planRepo, featOrderHistory, "List all orders for authenticated user", "ORD-004", "orders");
        UUID sLO1 = step(planRepo, tcListOrders, "Given", "I am authenticated as a customer with 3 past orders");
        UUID sLO2 = step(planRepo, tcListOrders, "When",  "I send GET /orders");
        UUID sLO3 = step(planRepo, tcListOrders, "Then",  "the response status code is 200");
        UUID sLO4 = step(planRepo, tcListOrders, "And",   "the response contains exactly 3 orders");

        UUID tcFilterOrders = scenario(planRepo, featOrderHistory, "Filter orders by date range returns correct subset", "ORD-005", "orders", "search");
        UUID sFO1 = step(planRepo, tcFilterOrders, "Given", "I am authenticated as a customer");
        UUID sFO2 = step(planRepo, tcFilterOrders, "When",  "I send GET /orders?from=2024-01-01&to=2024-06-30");
        UUID sFO3 = step(planRepo, tcFilterOrders, "Then",  "the response status code is 200");
        UUID sFO4 = step(planRepo, tcFilterOrders, "And",   "all returned orders have a date within the requested range");

        // ── Suite 4: Product Catalog ─────────────────────────────────────────
        UUID suiteCatalog = child(planRepo, root, NodeType.TEST_SUITE, null, "Product Catalog");

        UUID featSearch = child(planRepo, suiteCatalog, NodeType.TEST_FEATURE, "Feature", "Product Search");

        UUID tcSearchKeyword = scenario(planRepo, featSearch, "Search products by keyword returns matching results", "PRD-001", "search", "smoke");
        UUID sSK1 = step(planRepo, tcSearchKeyword, "Given", "the catalog contains products with 'laptop' in the name");
        UUID sSK2 = step(planRepo, tcSearchKeyword, "When",  "I send GET /products?q=laptop");
        UUID sSK3 = step(planRepo, tcSearchKeyword, "Then",  "the response status code is 200");
        UUID sSK4 = step(planRepo, tcSearchKeyword, "And",   "every result contains 'laptop' in name or description");

        UUID tcSearchEmpty = scenario(planRepo, featSearch, "Search with no results returns empty list", "PRD-002", "search", "negative");
        UUID sSE1 = step(planRepo, tcSearchEmpty, "Given", "no products match the keyword 'zzzyyyxxx'");
        UUID sSE2 = step(planRepo, tcSearchEmpty, "When",  "I send GET /products?q=zzzyyyxxx");
        UUID sSE3 = step(planRepo, tcSearchEmpty, "Then",  "the response status code is 200");
        UUID sSE4 = step(planRepo, tcSearchEmpty, "And",   "the response body contains an empty 'items' array");

        UUID tcFilterCategory = scenario(planRepo, featSearch, "Filter products by category", "PRD-003", "search");
        UUID sFC1 = step(planRepo, tcFilterCategory, "Given", "products in category 'electronics' exist");
        UUID sFC2 = step(planRepo, tcFilterCategory, "When",  "I send GET /products?category=electronics");
        UUID sFC3 = step(planRepo, tcFilterCategory, "Then",  "the response status code is 200");
        UUID sFC4 = step(planRepo, tcFilterCategory, "And",   "all returned products belong to 'electronics' category");

        UUID tcSortByPrice = scenario(planRepo, featSearch, "Sort products by price ascending", "PRD-004", "search");
        UUID sSP_1 = step(planRepo, tcSortByPrice, "When",  "I send GET /products?sort=price_asc");
        UUID sSP_2 = step(planRepo, tcSortByPrice, "Then",  "the response status code is 200");
        UUID sSP_3 = step(planRepo, tcSortByPrice, "And",   "prices in the response are in ascending order");

        UUID featProductDetail = child(planRepo, suiteCatalog, NodeType.TEST_FEATURE, "Feature", "Product Details");

        UUID tcGetProduct = scenario(planRepo, featProductDetail, "Get product detail by valid ID", "PRD-005", "smoke");
        UUID sGP1 = step(planRepo, tcGetProduct, "When",  "I send GET /products/SKU-100");
        UUID sGP2 = step(planRepo, tcGetProduct, "Then",  "the response status code is 200");
        UUID sGP3 = step(planRepo, tcGetProduct, "And",   "the response contains 'sku', 'name', 'price' and 'stock' fields");

        UUID tcGetProductNotFound = scenario(planRepo, featProductDetail, "Get non-existent product returns 404", "PRD-006", "negative");
        UUID sGPN1 = step(planRepo, tcGetProductNotFound, "When",  "I send GET /products/SKU-DOESNOTEXIST");
        UUID sGPN2 = step(planRepo, tcGetProductNotFound, "Then",  "the response status code is 404");

        // ── Suite 5: Notifications ────────────────────────────────────────────
        UUID suiteNotif = child(planRepo, root, NodeType.TEST_SUITE, null, "Notifications");

        UUID featEmail = child(planRepo, suiteNotif, NodeType.TEST_FEATURE, "Feature", "Email Notifications");

        UUID tcWelcomeEmail = scenario(planRepo, featEmail, "Welcome email is sent on user registration", "NOT-001", "notifications", "smoke");
        UUID sWE1 = step(planRepo, tcWelcomeEmail, "Given", "no user with email 'new@example.com' exists");
        UUID sWE2 = step(planRepo, tcWelcomeEmail, "When",  "I send POST /users to register 'new@example.com'");
        UUID sWE3 = step(planRepo, tcWelcomeEmail, "Then",  "the response status code is 201");
        UUID sWE4 = step(planRepo, tcWelcomeEmail, "And",   "a welcome email is delivered to 'new@example.com'");

        UUID tcPasswordReset = scenario(planRepo, featEmail, "Password reset email is sent to known address", "NOT-002", "notifications");
        UUID sPR1 = step(planRepo, tcPasswordReset, "Given", "a user with email 'admin@example.com' exists");
        UUID sPR2 = step(planRepo, tcPasswordReset, "When",  "I send POST /auth/forgot-password with that email");
        UUID sPR3 = step(planRepo, tcPasswordReset, "Then",  "the response status code is 202");
        UUID sPR4 = step(planRepo, tcPasswordReset, "And",   "a password reset email is delivered within 5 seconds");

        UUID tcResetUnknown = scenario(planRepo, featEmail, "Password reset for unknown email returns 404", "NOT-003", "notifications", "negative");
        UUID sPRU1 = step(planRepo, tcResetUnknown, "When",  "I send POST /auth/forgot-password with 'nobody@example.com'");
        UUID sPRU2 = step(planRepo, tcResetUnknown, "Then",  "the response status code is 404");

        UUID featPush = child(planRepo, suiteNotif, NodeType.TEST_FEATURE, "Feature", "Push Notifications");

        UUID tcPushOrder = scenario(planRepo, featPush, "Push notification sent on order status change", "NOT-004", "notifications", "push");
        UUID sPPN1 = step(planRepo, tcPushOrder, "Given", "a customer has a registered device token");
        UUID sPPN2 = step(planRepo, tcPushOrder, "And",   "they have a pending order");
        UUID sPPN3 = step(planRepo, tcPushOrder, "When",  "the order status changes to 'SHIPPED'");
        UUID sPPN4 = step(planRepo, tcPushOrder, "Then",  "a push notification is sent to the device");
        UUID sPPN5 = step(planRepo, tcPushOrder, "And",   "the notification body mentions the order ID");

        UUID tcPushInvalidToken = scenario(planRepo, featPush, "Push notification with invalid device token fails gracefully", "NOT-005", "notifications", "push", "negative");
        UUID sPIT1 = step(planRepo, tcPushInvalidToken, "Given", "a customer has an expired device token");
        UUID sPIT2 = step(planRepo, tcPushInvalidToken, "When",  "a notification is triggered for that device");
        UUID sPIT3 = step(planRepo, tcPushInvalidToken, "Then",  "the system logs a delivery failure");
        UUID sPIT4 = step(planRepo, tcPushInvalidToken, "And",   "no exception propagates to the caller");

        // ─── Persist plan ────────────────────────────────────────────────────
        TestPlan plan = planRepo.persistPlan(
            new TestPlan(null, projectID, Instant.now(), "rh", "ch", root, 22, null));
        planRepo.assignPlanToNodes(plan.planID(), root);

        // ---- create execution ----

        TestExecution execution = execRepo.newExecution(plan.planID(), Instant.now(), "ci");
        UUID execID = execution.executionID();
        Instant t = Instant.now().minus(8, ChronoUnit.MINUTES);

        long ms = 0;

        // ── Suite 1: Authentication → FAILED ──────────────────────────────────
        execNode(execRepo, execID, suiteAuth,  ExecutionResult.FAILED, t, 300_000);
        execNode(execRepo, execID, featLogin,  ExecutionResult.FAILED, t, 200_000);

        execNode(execRepo, execID, tcLogin,    ExecutionResult.PASSED, t.plusMillis(ms), 520);
        execNode(execRepo, execID, sL1,        ExecutionResult.PASSED, t.plusMillis(ms),  45);
        execNode(execRepo, execID, sL2,        ExecutionResult.PASSED, t.plusMillis(ms + 50), 380);
        execNode(execRepo, execID, sL3,        ExecutionResult.PASSED, t.plusMillis(ms + 440), 30);
        execNode(execRepo, execID, sL4,        ExecutionResult.PASSED, t.plusMillis(ms + 475), 30);
        ms += 600;

        execNode(execRepo, execID, tcBadPwd,   ExecutionResult.FAILED, t.plusMillis(ms), 310);
        execNode(execRepo, execID, sB1,        ExecutionResult.PASSED,  t.plusMillis(ms),  40);
        execNode(execRepo, execID, sB2,        ExecutionResult.PASSED,  t.plusMillis(ms + 50), 200);
        execNode(execRepo, execID, sB3,        ExecutionResult.FAILED,  t.plusMillis(ms + 260), 45);
        execNode(execRepo, execID, sB4,        ExecutionResult.SKIPPED, t.plusMillis(ms + 310),  0);
        ms += 400;

        execNode(execRepo, execID, tcLocked,   ExecutionResult.PASSED, t.plusMillis(ms), 480);
        execNode(execRepo, execID, sK1,        ExecutionResult.PASSED, t.plusMillis(ms),  30);
        execNode(execRepo, execID, sK2,        ExecutionResult.PASSED, t.plusMillis(ms + 35), 120);
        execNode(execRepo, execID, sK3,        ExecutionResult.PASSED, t.plusMillis(ms + 160), 290);
        execNode(execRepo, execID, sK4,        ExecutionResult.PASSED, t.plusMillis(ms + 455),  15);
        execNode(execRepo, execID, sK5,        ExecutionResult.PASSED, t.plusMillis(ms + 472),  10);
        ms += 550;

        execNode(execRepo, execID, featRefresh, ExecutionResult.PASSED, t.plusMillis(ms), 700);
        execNode(execRepo, execID, tcRefresh,   ExecutionResult.PASSED, t.plusMillis(ms), 590);
        execNode(execRepo, execID, sR1,         ExecutionResult.PASSED, t.plusMillis(ms),  50);
        execNode(execRepo, execID, sR2,         ExecutionResult.PASSED, t.plusMillis(ms + 55), 450);
        execNode(execRepo, execID, sR3,         ExecutionResult.PASSED, t.plusMillis(ms + 510), 60);
        execNode(execRepo, execID, sR4,         ExecutionResult.PASSED, t.plusMillis(ms + 575), 25);
        ms += 700;

        execNode(execRepo, execID, tcRefreshInvalid, ExecutionResult.PASSED, t.plusMillis(ms), 210);
        execNode(execRepo, execID, sRI1,             ExecutionResult.PASSED, t.plusMillis(ms),  35);
        execNode(execRepo, execID, sRI2,             ExecutionResult.PASSED, t.plusMillis(ms + 40), 140);
        execNode(execRepo, execID, sRI3,             ExecutionResult.PASSED, t.plusMillis(ms + 185), 20);
        ms += 300;

        execNode(execRepo, execID, featLogout, ExecutionResult.PASSED, t.plusMillis(ms), 500);
        execNode(execRepo, execID, tcLogout,   ExecutionResult.PASSED, t.plusMillis(ms), 490);
        execNode(execRepo, execID, sO1,        ExecutionResult.PASSED, t.plusMillis(ms),  40);
        execNode(execRepo, execID, sO2,        ExecutionResult.PASSED, t.plusMillis(ms + 45), 320);
        execNode(execRepo, execID, sO3,        ExecutionResult.PASSED, t.plusMillis(ms + 370), 60);
        execNode(execRepo, execID, sO4,        ExecutionResult.PASSED, t.plusMillis(ms + 435), 55);
        ms += 600;

        // ── Suite 2: User Management → ERROR ──────────────────────────────────
        execNode(execRepo, execID, suiteUsers,   ExecutionResult.ERROR,  t.plusMillis(ms), 250_000);
        execNode(execRepo, execID, featUserCrud, ExecutionResult.ERROR,  t.plusMillis(ms), 200_000);

        execNode(execRepo, execID, tcCreate,     ExecutionResult.PASSED, t.plusMillis(ms), 800);
        execNode(execRepo, execID, sC1,          ExecutionResult.PASSED, t.plusMillis(ms),  55);
        execNode(execRepo, execID, sC2,          ExecutionResult.PASSED, t.plusMillis(ms + 60), 620);
        execNode(execRepo, execID, sC3,          ExecutionResult.PASSED, t.plusMillis(ms + 685), 70);
        execNode(execRepo, execID, sC4,          ExecutionResult.PASSED, t.plusMillis(ms + 760), 35);
        ms += 900;

        execNode(execRepo, execID, tcUpdate,     ExecutionResult.PASSED, t.plusMillis(ms), 650);
        execNode(execRepo, execID, sU1,          ExecutionResult.PASSED, t.plusMillis(ms),  40);
        execNode(execRepo, execID, sU2,          ExecutionResult.PASSED, t.plusMillis(ms + 45), 80);
        execNode(execRepo, execID, sU3,          ExecutionResult.PASSED, t.plusMillis(ms + 130), 430);
        execNode(execRepo, execID, sU4,          ExecutionResult.PASSED, t.plusMillis(ms + 565), 55);
        execNode(execRepo, execID, sU5,          ExecutionResult.PASSED, t.plusMillis(ms + 625), 30);
        ms += 750;

        execNode(execRepo, execID, tcDelete,     ExecutionResult.ERROR,  t.plusMillis(ms), 500);
        execNode(execRepo, execID, sD1,          ExecutionResult.PASSED, t.plusMillis(ms),  50);
        execNode(execRepo, execID, sD2,          ExecutionResult.ERROR,  t.plusMillis(ms + 55), 440);
        execNode(execRepo, execID, sD3,          ExecutionResult.SKIPPED, t.plusMillis(ms + 500), 0);
        ms += 600;

        execNode(execRepo, execID, featRoles,    ExecutionResult.PASSED, t.plusMillis(ms), 900);
        execNode(execRepo, execID, tcPromote,    ExecutionResult.PASSED, t.plusMillis(ms), 560);
        execNode(execRepo, execID, sP1,          ExecutionResult.PASSED, t.plusMillis(ms),  30);
        execNode(execRepo, execID, sP2,          ExecutionResult.PASSED, t.plusMillis(ms + 35), 60);
        execNode(execRepo, execID, sP3,          ExecutionResult.PASSED, t.plusMillis(ms + 100), 380);
        execNode(execRepo, execID, sP4,          ExecutionResult.PASSED, t.plusMillis(ms + 485), 50);
        execNode(execRepo, execID, sP5,          ExecutionResult.PASSED, t.plusMillis(ms + 540), 25);
        ms += 650;

        execNode(execRepo, execID, tcSelfPromote, ExecutionResult.PASSED, t.plusMillis(ms), 230);
        execNode(execRepo, execID, sSP1,          ExecutionResult.PASSED, t.plusMillis(ms),  35);
        execNode(execRepo, execID, sSP2,          ExecutionResult.PASSED, t.plusMillis(ms + 40), 165);
        execNode(execRepo, execID, sSP3,          ExecutionResult.PASSED, t.plusMillis(ms + 210), 20);
        ms += 300;

        // ── Suite 3: Order Management → FAILED ────────────────────────────────
        execNode(execRepo, execID, suiteOrders,       ExecutionResult.FAILED, t.plusMillis(ms), 400_000);
        execNode(execRepo, execID, featOrderPlacement, ExecutionResult.FAILED, t.plusMillis(ms), 300_000);

        execNode(execRepo, execID, tcPlaceOrder,  ExecutionResult.PASSED, t.plusMillis(ms), 1_200);
        execNode(execRepo, execID, sPO1,          ExecutionResult.PASSED, t.plusMillis(ms),  40);
        execNode(execRepo, execID, sPO2,          ExecutionResult.PASSED, t.plusMillis(ms + 45), 80);
        execNode(execRepo, execID, sPO3,          ExecutionResult.PASSED, t.plusMillis(ms + 130), 850);
        execNode(execRepo, execID, sPO4,          ExecutionResult.PASSED, t.plusMillis(ms + 985), 90);
        execNode(execRepo, execID, sPO5,          ExecutionResult.PASSED, t.plusMillis(ms + 1_080), 60);
        execNode(execRepo, execID, sPO6,          ExecutionResult.PASSED, t.plusMillis(ms + 1_145), 50);
        ms += 1_300;

        execNode(execRepo, execID, tcOutOfStock,  ExecutionResult.FAILED, t.plusMillis(ms), 420);
        execNode(execRepo, execID, sOS1,          ExecutionResult.PASSED,  t.plusMillis(ms),  35);
        execNode(execRepo, execID, sOS2,          ExecutionResult.PASSED,  t.plusMillis(ms + 40), 90);
        execNode(execRepo, execID, sOS3,          ExecutionResult.PASSED,  t.plusMillis(ms + 135), 250);
        execNode(execRepo, execID, sOS4,          ExecutionResult.FAILED,  t.plusMillis(ms + 390), 25);
        execNode(execRepo, execID, sOS5,          ExecutionResult.SKIPPED, t.plusMillis(ms + 420),  0);
        ms += 500;

        execNode(execRepo, execID, tcCancelOrder, ExecutionResult.PASSED, t.plusMillis(ms), 380);
        execNode(execRepo, execID, sCO1,          ExecutionResult.PASSED, t.plusMillis(ms),  30);
        execNode(execRepo, execID, sCO2,          ExecutionResult.PASSED, t.plusMillis(ms + 35), 60);
        execNode(execRepo, execID, sCO3,          ExecutionResult.PASSED, t.plusMillis(ms + 100), 220);
        execNode(execRepo, execID, sCO4,          ExecutionResult.PASSED, t.plusMillis(ms + 325), 30);
        execNode(execRepo, execID, sCO5,          ExecutionResult.PASSED, t.plusMillis(ms + 360), 20);
        ms += 450;

        execNode(execRepo, execID, featOrderHistory, ExecutionResult.PASSED, t.plusMillis(ms), 600);
        execNode(execRepo, execID, tcListOrders,     ExecutionResult.PASSED, t.plusMillis(ms), 290);
        execNode(execRepo, execID, sLO1,             ExecutionResult.PASSED, t.plusMillis(ms),  40);
        execNode(execRepo, execID, sLO2,             ExecutionResult.PASSED, t.plusMillis(ms + 45), 200);
        execNode(execRepo, execID, sLO3,             ExecutionResult.PASSED, t.plusMillis(ms + 250), 25);
        execNode(execRepo, execID, sLO4,             ExecutionResult.PASSED, t.plusMillis(ms + 280), 15);
        ms += 350;

        execNode(execRepo, execID, tcFilterOrders,   ExecutionResult.PASSED, t.plusMillis(ms), 310);
        execNode(execRepo, execID, sFO1,             ExecutionResult.PASSED, t.plusMillis(ms),  30);
        execNode(execRepo, execID, sFO2,             ExecutionResult.PASSED, t.plusMillis(ms + 35), 215);
        execNode(execRepo, execID, sFO3,             ExecutionResult.PASSED, t.plusMillis(ms + 255), 30);
        execNode(execRepo, execID, sFO4,             ExecutionResult.PASSED, t.plusMillis(ms + 290), 20);
        ms += 400;

        // ── Suite 4: Product Catalog → FAILED ────────────────────────────────
        execNode(execRepo, execID, suiteCatalog, ExecutionResult.FAILED, t.plusMillis(ms), 350_000);
        execNode(execRepo, execID, featSearch,   ExecutionResult.FAILED, t.plusMillis(ms), 280_000);

        execNode(execRepo, execID, tcSearchKeyword, ExecutionResult.PASSED, t.plusMillis(ms), 450);
        execNode(execRepo, execID, sSK1,            ExecutionResult.PASSED, t.plusMillis(ms),  40);
        execNode(execRepo, execID, sSK2,            ExecutionResult.PASSED, t.plusMillis(ms + 45), 360);
        execNode(execRepo, execID, sSK3,            ExecutionResult.PASSED, t.plusMillis(ms + 410), 25);
        execNode(execRepo, execID, sSK4,            ExecutionResult.PASSED, t.plusMillis(ms + 440), 15);
        ms += 520;

        execNode(execRepo, execID, tcSearchEmpty,   ExecutionResult.PASSED, t.plusMillis(ms), 390);
        execNode(execRepo, execID, sSE1,            ExecutionResult.PASSED, t.plusMillis(ms),  30);
        execNode(execRepo, execID, sSE2,            ExecutionResult.PASSED, t.plusMillis(ms + 35), 310);
        execNode(execRepo, execID, sSE3,            ExecutionResult.PASSED, t.plusMillis(ms + 350), 20);
        execNode(execRepo, execID, sSE4,            ExecutionResult.PASSED, t.plusMillis(ms + 375), 15);
        ms += 450;

        execNode(execRepo, execID, tcFilterCategory, ExecutionResult.FAILED, t.plusMillis(ms), 520);
        execNode(execRepo, execID, sFC1,             ExecutionResult.PASSED, t.plusMillis(ms),  35);
        execNode(execRepo, execID, sFC2,             ExecutionResult.PASSED, t.plusMillis(ms + 40), 420);
        execNode(execRepo, execID, sFC3,             ExecutionResult.FAILED, t.plusMillis(ms + 465), 50);
        execNode(execRepo, execID, sFC4,             ExecutionResult.SKIPPED, t.plusMillis(ms + 520), 0);
        ms += 600;

        execNode(execRepo, execID, tcSortByPrice,   ExecutionResult.PASSED, t.plusMillis(ms), 340);
        execNode(execRepo, execID, sSP_1,           ExecutionResult.PASSED, t.plusMillis(ms), 290);
        execNode(execRepo, execID, sSP_2,           ExecutionResult.PASSED, t.plusMillis(ms + 295), 30);
        execNode(execRepo, execID, sSP_3,           ExecutionResult.PASSED, t.plusMillis(ms + 330), 15);
        ms += 400;

        execNode(execRepo, execID, featProductDetail,  ExecutionResult.PASSED, t.plusMillis(ms), 400);
        execNode(execRepo, execID, tcGetProduct,       ExecutionResult.PASSED, t.plusMillis(ms), 260);
        execNode(execRepo, execID, sGP1,               ExecutionResult.PASSED, t.plusMillis(ms), 200);
        execNode(execRepo, execID, sGP2,               ExecutionResult.PASSED, t.plusMillis(ms + 205), 30);
        execNode(execRepo, execID, sGP3,               ExecutionResult.PASSED, t.plusMillis(ms + 240), 25);
        ms += 320;

        execNode(execRepo, execID, tcGetProductNotFound, ExecutionResult.PASSED, t.plusMillis(ms), 180);
        execNode(execRepo, execID, sGPN1,               ExecutionResult.PASSED, t.plusMillis(ms), 150);
        execNode(execRepo, execID, sGPN2,               ExecutionResult.PASSED, t.plusMillis(ms + 155), 25);
        ms += 250;

        // ── Suite 5: Notifications → PASSED ──────────────────────────────────
        execNode(execRepo, execID, suiteNotif, ExecutionResult.PASSED, t.plusMillis(ms), 600_000);
        execNode(execRepo, execID, featEmail,  ExecutionResult.PASSED, t.plusMillis(ms), 400_000);

        execNode(execRepo, execID, tcWelcomeEmail, ExecutionResult.PASSED, t.plusMillis(ms), 1_100);
        execNode(execRepo, execID, sWE1,           ExecutionResult.PASSED, t.plusMillis(ms),  40);
        execNode(execRepo, execID, sWE2,           ExecutionResult.PASSED, t.plusMillis(ms + 45), 650);
        execNode(execRepo, execID, sWE3,           ExecutionResult.PASSED, t.plusMillis(ms + 700), 80);
        execNode(execRepo, execID, sWE4,           ExecutionResult.PASSED, t.plusMillis(ms + 785), 310);
        ms += 1_200;

        execNode(execRepo, execID, tcPasswordReset, ExecutionResult.PASSED, t.plusMillis(ms), 1_350);
        execNode(execRepo, execID, sPR1,            ExecutionResult.PASSED, t.plusMillis(ms),  35);
        execNode(execRepo, execID, sPR2,            ExecutionResult.PASSED, t.plusMillis(ms + 40), 580);
        execNode(execRepo, execID, sPR3,            ExecutionResult.PASSED, t.plusMillis(ms + 625), 70);
        execNode(execRepo, execID, sPR4,            ExecutionResult.PASSED, t.plusMillis(ms + 700), 645);
        ms += 1_400;

        execNode(execRepo, execID, tcResetUnknown,  ExecutionResult.PASSED, t.plusMillis(ms), 290);
        execNode(execRepo, execID, sPRU1,           ExecutionResult.PASSED, t.plusMillis(ms), 250);
        execNode(execRepo, execID, sPRU2,           ExecutionResult.PASSED, t.plusMillis(ms + 255), 30);
        ms += 350;

        execNode(execRepo, execID, featPush,        ExecutionResult.PASSED, t.plusMillis(ms), 800);
        execNode(execRepo, execID, tcPushOrder,     ExecutionResult.PASSED, t.plusMillis(ms), 620);
        execNode(execRepo, execID, sPPN1,           ExecutionResult.PASSED, t.plusMillis(ms),  30);
        execNode(execRepo, execID, sPPN2,           ExecutionResult.PASSED, t.plusMillis(ms + 35), 50);
        execNode(execRepo, execID, sPPN3,           ExecutionResult.PASSED, t.plusMillis(ms + 90), 380);
        execNode(execRepo, execID, sPPN4,           ExecutionResult.PASSED, t.plusMillis(ms + 475), 95);
        execNode(execRepo, execID, sPPN5,           ExecutionResult.PASSED, t.plusMillis(ms + 575), 40);
        ms += 700;

        execNode(execRepo, execID, tcPushInvalidToken, ExecutionResult.PASSED, t.plusMillis(ms), 480);
        execNode(execRepo, execID, sPIT1,              ExecutionResult.PASSED, t.plusMillis(ms),  35);
        execNode(execRepo, execID, sPIT2,              ExecutionResult.PASSED, t.plusMillis(ms + 40), 290);
        execNode(execRepo, execID, sPIT3,              ExecutionResult.PASSED, t.plusMillis(ms + 335), 85);
        execNode(execRepo, execID, sPIT4,              ExecutionResult.PASSED, t.plusMillis(ms + 425), 50);

        // totals: 18 passed, 3 failed, 1 error  (22 total)
        execRepo.updateExecutionTestCounts(execID, 18, 3, 1);

        // ---- generate report ----

        ReportBuilder builder = runtime.getExtensions(ReportBuilder.class).findFirst().orElseThrow();
        builder.buildReport(execID);

        // ---- verify ----

        Path pdf = Files.list(OUTPUT_DIR)
            .filter(p -> p.getFileName().toString().endsWith(".pdf"))
            .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
            .orElseThrow(() -> new AssertionError("No PDF found in " + OUTPUT_DIR));
        assertThat(pdf.toFile().length()).isGreaterThan(0);
        System.out.println("\n>>> PDF report: " + pdf.toAbsolutePath() + "\n");
    }

    // ---- plan-building helpers ----

    private UUID child(TestPlanRepository repo, UUID parent, NodeType type, String keyword, String name) {
        TestPlanNode node = new TestPlanNode().nodeType(type).name(name);
        if (keyword != null) node.keyword(keyword);
        UUID id = repo.persistNode(node);
        repo.attachChildNodeLast(parent, id);
        return id;
    }

    private UUID scenario(TestPlanRepository repo, UUID parent, String name,
                           String identifier, String... tags) {
        TestPlanNode node = new TestPlanNode().nodeType(NodeType.TEST_CASE).keyword("Scenario").name(name);
        if (identifier != null) node.identifier(identifier);
        for (String tag : tags) node.addTag(tag);
        UUID id = repo.persistNode(node);
        repo.attachChildNodeLast(parent, id);
        return id;
    }

    private UUID step(TestPlanRepository repo, UUID parent, String keyword, String name) {
        UUID id = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).keyword(keyword).name(name));
        repo.attachChildNodeLast(parent, id);
        return id;
    }

    private UUID stepWithDoc(TestPlanRepository repo, UUID parent, String keyword, String name,
                              String mimeType, String content) {
        UUID id = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).keyword(keyword).name(name)
            .document(Document.of(mimeType, content)));
        repo.attachChildNodeLast(parent, id);
        return id;
    }

    private UUID stepWithTable(TestPlanRepository repo, UUID parent, String keyword, String name,
                                List<List<String>> rows) {
        UUID id = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).keyword(keyword).name(name)
            .dataTable(new DataTable(rows)));
        repo.attachChildNodeLast(parent, id);
        return id;
    }

    // ---- logo helper ----

    private void generateTestLogo(Path dest) throws Exception {
        int w = 260, h = 70;
        var img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g   = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Square icon
        g.setColor(new java.awt.Color(0x1C, 0x72, 0xC7));
        g.fillRoundRect(0, 0, 70, 70, 18, 18);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 44));
        g.drawString("A", 16, 55);

        // Company name — white so it's visible on dark header background
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 30));
        g.drawString("ACME", 84, 38);

        // Tagline — light gray
        g.setColor(new java.awt.Color(0xCC, 0xCC, 0xCC));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        g.drawString("Corporation", 86, 58);

        g.dispose();
        javax.imageio.ImageIO.write(img, "PNG", dest.toFile());
    }

    // ---- execution helpers ----

    private UUID execNode(TestExecutionRepository repo, UUID execID, UUID planNodeID,
                           ExecutionResult result, Instant start, long durationMs) {
        UUID id = repo.newExecutionNode(execID, planNodeID);
        repo.updateExecutionNodeStart(id, start);
        repo.updateExecutionNodeFinish(id, result, start.plusMillis(durationMs));
        return id;
    }

}