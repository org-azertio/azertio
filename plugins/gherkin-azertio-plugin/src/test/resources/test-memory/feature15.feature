# Memory test feature file 15
# featureProperty: MemTest15
@MemoryTest15
Feature: Memory Test 15 - Complex Scenarios
  This feature file is designed to test memory consumption.
  It contains multiple scenarios with various step types.
  Feature number 15 of the memory test suite.

Background:
  Given the system is initialized
  And the database connection is established
  And the test environment is configured for feature 15

# Scenario 1 comment block
# scenarioProperty: Prop15_1
@ID-MemTest15_Scenario1 @Priority_High @Category_Functional
Scenario: Scenario 1 of feature 15 - Validate operation 1
  # Step comments for scenario 1
  # stepProperty: StepProp15_1
  Given user 1 is logged in with role admin for feature 15
  And the initial balance is 100.50 in account ACC-15-1
  And the transaction limit is set to 1000
  When a transfer of 50.25 is initiated from ACC-15-1 to ACC-15-11
  And the transfer is confirmed with token TK-15-1
  Then the balance of ACC-15-1 should be 50.25
  And the transaction log should contain entry for transfer 1
  And an email notification should be sent to user 1

# Scenario 2 comment block
# scenarioProperty: Prop15_2
@ID-MemTest15_Scenario2 @Priority_High @Category_Functional
Scenario: Scenario 2 of feature 15 - Validate operation 2
  # Step comments for scenario 2
  # stepProperty: StepProp15_2
  Given user 2 is logged in with role admin for feature 15
  And the initial balance is 200.50 in account ACC-15-2
  And the transaction limit is set to 2000
  When a transfer of 100.25 is initiated from ACC-15-2 to ACC-15-12
  And the transfer is confirmed with token TK-15-2
  Then the balance of ACC-15-2 should be 100.25
  And the transaction log should contain entry for transfer 2
  And an email notification should be sent to user 2

# Scenario 3 comment block
# scenarioProperty: Prop15_3
@ID-MemTest15_Scenario3 @Priority_High @Category_Functional
Scenario: Scenario 3 of feature 15 - Validate operation 3
  # Step comments for scenario 3
  # stepProperty: StepProp15_3
  Given user 3 is logged in with role admin for feature 15
  And the initial balance is 300.50 in account ACC-15-3
  And the transaction limit is set to 3000
  When a transfer of 150.25 is initiated from ACC-15-3 to ACC-15-13
  And the transfer is confirmed with token TK-15-3
  Then the balance of ACC-15-3 should be 150.25
  And the transaction log should contain entry for transfer 3
  And an email notification should be sent to user 3

# Scenario 4 comment block
# scenarioProperty: Prop15_4
@ID-MemTest15_Scenario4 @Priority_High @Category_Functional
Scenario: Scenario 4 of feature 15 - Validate operation 4
  # Step comments for scenario 4
  # stepProperty: StepProp15_4
  Given user 4 is logged in with role admin for feature 15
  And the initial balance is 400.50 in account ACC-15-4
  And the transaction limit is set to 4000
  When a transfer of 200.25 is initiated from ACC-15-4 to ACC-15-14
  And the transfer is confirmed with token TK-15-4
  Then the balance of ACC-15-4 should be 200.25
  And the transaction log should contain entry for transfer 4
  And an email notification should be sent to user 4

# Scenario 5 comment block
# scenarioProperty: Prop15_5
@ID-MemTest15_Scenario5 @Priority_High @Category_Functional
Scenario: Scenario 5 of feature 15 - Validate operation 5
  # Step comments for scenario 5
  # stepProperty: StepProp15_5
  Given user 5 is logged in with role admin for feature 15
  And the initial balance is 500.50 in account ACC-15-5
  And the transaction limit is set to 5000
  When a transfer of 250.25 is initiated from ACC-15-5 to ACC-15-15
  And the transfer is confirmed with token TK-15-5
  Then the balance of ACC-15-5 should be 250.25
  And the transaction log should contain entry for transfer 5
  And an email notification should be sent to user 5

# Scenario 6 comment block
# scenarioProperty: Prop15_6
@ID-MemTest15_Scenario6 @Priority_High @Category_Functional
Scenario: Scenario 6 of feature 15 - Validate operation 6
  # Step comments for scenario 6
  # stepProperty: StepProp15_6
  Given user 6 is logged in with role admin for feature 15
  And the initial balance is 600.50 in account ACC-15-6
  And the transaction limit is set to 6000
  When a transfer of 300.25 is initiated from ACC-15-6 to ACC-15-16
  And the transfer is confirmed with token TK-15-6
  Then the balance of ACC-15-6 should be 300.25
  And the transaction log should contain entry for transfer 6
  And an email notification should be sent to user 6

# Scenario 7 comment block
# scenarioProperty: Prop15_7
@ID-MemTest15_Scenario7 @Priority_High @Category_Functional
Scenario: Scenario 7 of feature 15 - Validate operation 7
  # Step comments for scenario 7
  # stepProperty: StepProp15_7
  Given user 7 is logged in with role admin for feature 15
  And the initial balance is 700.50 in account ACC-15-7
  And the transaction limit is set to 7000
  When a transfer of 350.25 is initiated from ACC-15-7 to ACC-15-17
  And the transfer is confirmed with token TK-15-7
  Then the balance of ACC-15-7 should be 350.25
  And the transaction log should contain entry for transfer 7
  And an email notification should be sent to user 7

# Scenario 8 comment block
# scenarioProperty: Prop15_8
@ID-MemTest15_Scenario8 @Priority_High @Category_Functional
Scenario: Scenario 8 of feature 15 - Validate operation 8
  # Step comments for scenario 8
  # stepProperty: StepProp15_8
  Given user 8 is logged in with role admin for feature 15
  And the initial balance is 800.50 in account ACC-15-8
  And the transaction limit is set to 8000
  When a transfer of 400.25 is initiated from ACC-15-8 to ACC-15-18
  And the transfer is confirmed with token TK-15-8
  Then the balance of ACC-15-8 should be 400.25
  And the transaction log should contain entry for transfer 8
  And an email notification should be sent to user 8

@ID-MemTest15_Outline1 @Priority_Medium @Category_DataDriven
Scenario Outline: Outline 1 of feature 15 - Parameterized validation
  Given a user with name <userName> and role <userRole>
  And an account <accountId> with balance <initialBalance>
  And the daily limit is <dailyLimit>
  When a <transactionType> of <amount> is processed
  And the operation is verified with code <verificationCode>
  Then the final balance should be <expectedBalance>
  And the status should be <expectedStatus>
  And the audit log should record <auditEntry>
  Examples:
    | userName   | userRole | accountId    | initialBalance | dailyLimit | transactionType | amount  | verificationCode | expectedBalance | expectedStatus | auditEntry          |
    | User01F15 | role_1  | ACC-15-O1-01 | 110.00         | 1000      | transfer        | 30.00 | VC-15-1-01      | 80.00         | completed      | AUDIT-15-1-01  |
    | User02F15 | role_2  | ACC-15-O1-02 | 210.00         | 2000      | transfer        | 55.00 | VC-15-1-02      | 155.00         | completed      | AUDIT-15-1-02  |
    | User03F15 | role_3  | ACC-15-O1-03 | 310.00         | 3000      | transfer        | 80.00 | VC-15-1-03      | 230.00         | completed      | AUDIT-15-1-03  |
    | User04F15 | role_4  | ACC-15-O1-04 | 410.00         | 4000      | transfer        | 105.00 | VC-15-1-04      | 305.00         | completed      | AUDIT-15-1-04  |
    | User05F15 | role_5  | ACC-15-O1-05 | 510.00         | 5000      | transfer        | 130.00 | VC-15-1-05      | 380.00         | completed      | AUDIT-15-1-05  |
    | User06F15 | role_6  | ACC-15-O1-06 | 610.00         | 6000      | transfer        | 155.00 | VC-15-1-06      | 455.00         | completed      | AUDIT-15-1-06  |
    | User07F15 | role_7  | ACC-15-O1-07 | 710.00         | 7000      | transfer        | 180.00 | VC-15-1-07      | 530.00         | completed      | AUDIT-15-1-07  |
    | User08F15 | role_8  | ACC-15-O1-08 | 810.00         | 8000      | transfer        | 205.00 | VC-15-1-08      | 605.00         | completed      | AUDIT-15-1-08  |
    | User09F15 | role_9  | ACC-15-O1-09 | 910.00         | 9000      | transfer        | 230.00 | VC-15-1-09      | 680.00         | completed      | AUDIT-15-1-09  |
    | User10F15 | role_10  | ACC-15-O1-10 | 1010.00         | 10000      | transfer        | 255.00 | VC-15-1-10      | 755.00         | completed      | AUDIT-15-1-10  |

@ID-MemTest15_Outline2 @Priority_Medium @Category_DataDriven
Scenario Outline: Outline 2 of feature 15 - Parameterized validation
  Given a user with name <userName> and role <userRole>
  And an account <accountId> with balance <initialBalance>
  And the daily limit is <dailyLimit>
  When a <transactionType> of <amount> is processed
  And the operation is verified with code <verificationCode>
  Then the final balance should be <expectedBalance>
  And the status should be <expectedStatus>
  And the audit log should record <auditEntry>
  Examples:
    | userName   | userRole | accountId    | initialBalance | dailyLimit | transactionType | amount  | verificationCode | expectedBalance | expectedStatus | auditEntry          |
    | User01F15 | role_1  | ACC-15-O2-01 | 120.00         | 1000      | transfer        | 35.00 | VC-15-2-01      | 85.00         | completed      | AUDIT-15-2-01  |
    | User02F15 | role_2  | ACC-15-O2-02 | 220.00         | 2000      | transfer        | 60.00 | VC-15-2-02      | 160.00         | completed      | AUDIT-15-2-02  |
    | User03F15 | role_3  | ACC-15-O2-03 | 320.00         | 3000      | transfer        | 85.00 | VC-15-2-03      | 235.00         | completed      | AUDIT-15-2-03  |
    | User04F15 | role_4  | ACC-15-O2-04 | 420.00         | 4000      | transfer        | 110.00 | VC-15-2-04      | 310.00         | completed      | AUDIT-15-2-04  |
    | User05F15 | role_5  | ACC-15-O2-05 | 520.00         | 5000      | transfer        | 135.00 | VC-15-2-05      | 385.00         | completed      | AUDIT-15-2-05  |
    | User06F15 | role_6  | ACC-15-O2-06 | 620.00         | 6000      | transfer        | 160.00 | VC-15-2-06      | 460.00         | completed      | AUDIT-15-2-06  |
    | User07F15 | role_7  | ACC-15-O2-07 | 720.00         | 7000      | transfer        | 185.00 | VC-15-2-07      | 535.00         | completed      | AUDIT-15-2-07  |
    | User08F15 | role_8  | ACC-15-O2-08 | 820.00         | 8000      | transfer        | 210.00 | VC-15-2-08      | 610.00         | completed      | AUDIT-15-2-08  |
    | User09F15 | role_9  | ACC-15-O2-09 | 920.00         | 9000      | transfer        | 235.00 | VC-15-2-09      | 685.00         | completed      | AUDIT-15-2-09  |
    | User10F15 | role_10  | ACC-15-O2-10 | 1020.00         | 10000      | transfer        | 260.00 | VC-15-2-10      | 760.00         | completed      | AUDIT-15-2-10  |

@ID-MemTest15_Outline3 @Priority_Medium @Category_DataDriven
Scenario Outline: Outline 3 of feature 15 - Parameterized validation
  Given a user with name <userName> and role <userRole>
  And an account <accountId> with balance <initialBalance>
  And the daily limit is <dailyLimit>
  When a <transactionType> of <amount> is processed
  And the operation is verified with code <verificationCode>
  Then the final balance should be <expectedBalance>
  And the status should be <expectedStatus>
  And the audit log should record <auditEntry>
  Examples:
    | userName   | userRole | accountId    | initialBalance | dailyLimit | transactionType | amount  | verificationCode | expectedBalance | expectedStatus | auditEntry          |
    | User01F15 | role_1  | ACC-15-O3-01 | 130.00         | 1000      | transfer        | 40.00 | VC-15-3-01      | 90.00         | completed      | AUDIT-15-3-01  |
    | User02F15 | role_2  | ACC-15-O3-02 | 230.00         | 2000      | transfer        | 65.00 | VC-15-3-02      | 165.00         | completed      | AUDIT-15-3-02  |
    | User03F15 | role_3  | ACC-15-O3-03 | 330.00         | 3000      | transfer        | 90.00 | VC-15-3-03      | 240.00         | completed      | AUDIT-15-3-03  |
    | User04F15 | role_4  | ACC-15-O3-04 | 430.00         | 4000      | transfer        | 115.00 | VC-15-3-04      | 315.00         | completed      | AUDIT-15-3-04  |
    | User05F15 | role_5  | ACC-15-O3-05 | 530.00         | 5000      | transfer        | 140.00 | VC-15-3-05      | 390.00         | completed      | AUDIT-15-3-05  |
    | User06F15 | role_6  | ACC-15-O3-06 | 630.00         | 6000      | transfer        | 165.00 | VC-15-3-06      | 465.00         | completed      | AUDIT-15-3-06  |
    | User07F15 | role_7  | ACC-15-O3-07 | 730.00         | 7000      | transfer        | 190.00 | VC-15-3-07      | 540.00         | completed      | AUDIT-15-3-07  |
    | User08F15 | role_8  | ACC-15-O3-08 | 830.00         | 8000      | transfer        | 215.00 | VC-15-3-08      | 615.00         | completed      | AUDIT-15-3-08  |
    | User09F15 | role_9  | ACC-15-O3-09 | 930.00         | 9000      | transfer        | 240.00 | VC-15-3-09      | 690.00         | completed      | AUDIT-15-3-09  |
    | User10F15 | role_10  | ACC-15-O3-10 | 1030.00         | 10000      | transfer        | 265.00 | VC-15-3-10      | 765.00         | completed      | AUDIT-15-3-10  |

@ID-MemTest15_Outline4 @Priority_Medium @Category_DataDriven
Scenario Outline: Outline 4 of feature 15 - Parameterized validation
  Given a user with name <userName> and role <userRole>
  And an account <accountId> with balance <initialBalance>
  And the daily limit is <dailyLimit>
  When a <transactionType> of <amount> is processed
  And the operation is verified with code <verificationCode>
  Then the final balance should be <expectedBalance>
  And the status should be <expectedStatus>
  And the audit log should record <auditEntry>
  Examples:
    | userName   | userRole | accountId    | initialBalance | dailyLimit | transactionType | amount  | verificationCode | expectedBalance | expectedStatus | auditEntry          |
    | User01F15 | role_1  | ACC-15-O4-01 | 140.00         | 1000      | transfer        | 45.00 | VC-15-4-01      | 95.00         | completed      | AUDIT-15-4-01  |
    | User02F15 | role_2  | ACC-15-O4-02 | 240.00         | 2000      | transfer        | 70.00 | VC-15-4-02      | 170.00         | completed      | AUDIT-15-4-02  |
    | User03F15 | role_3  | ACC-15-O4-03 | 340.00         | 3000      | transfer        | 95.00 | VC-15-4-03      | 245.00         | completed      | AUDIT-15-4-03  |
    | User04F15 | role_4  | ACC-15-O4-04 | 440.00         | 4000      | transfer        | 120.00 | VC-15-4-04      | 320.00         | completed      | AUDIT-15-4-04  |
    | User05F15 | role_5  | ACC-15-O4-05 | 540.00         | 5000      | transfer        | 145.00 | VC-15-4-05      | 395.00         | completed      | AUDIT-15-4-05  |
    | User06F15 | role_6  | ACC-15-O4-06 | 640.00         | 6000      | transfer        | 170.00 | VC-15-4-06      | 470.00         | completed      | AUDIT-15-4-06  |
    | User07F15 | role_7  | ACC-15-O4-07 | 740.00         | 7000      | transfer        | 195.00 | VC-15-4-07      | 545.00         | completed      | AUDIT-15-4-07  |
    | User08F15 | role_8  | ACC-15-O4-08 | 840.00         | 8000      | transfer        | 220.00 | VC-15-4-08      | 620.00         | completed      | AUDIT-15-4-08  |
    | User09F15 | role_9  | ACC-15-O4-09 | 940.00         | 9000      | transfer        | 245.00 | VC-15-4-09      | 695.00         | completed      | AUDIT-15-4-09  |
    | User10F15 | role_10  | ACC-15-O4-10 | 1040.00         | 10000      | transfer        | 270.00 | VC-15-4-10      | 770.00         | completed      | AUDIT-15-4-10  |
