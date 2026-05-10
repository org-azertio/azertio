Feature: Rfam Public Database
  Queries against the Rfam database (https://rfam.org), a public read-only MySQL
  database maintained by the European Bioinformatics Institute (EBI).
  Connection: mysql-rfam-public.ebi.ac.uk:4497 / user: rfamro

  Background:
    Given use db "rfam"

  @ID-DB-01 @DB
  Scenario: The family table contains a large number of RNA families
    Then count db table family > 3000

  @ID-DB-02 @DB
  Scenario: The 5S rRNA family (RF00001) exists
    When db query:
      """sql
      SELECT rfam_id, description FROM family WHERE rfam_acc = 'RF00001'
      """
    Then db query count = 1

  @ID-DB-03 @DB
  Scenario: All RNA families have a non-empty description
    When db query:
      """sql
      SELECT rfam_acc FROM family WHERE description IS NULL OR description = ''
      """
    Then db query count = 0

  @ID-DB-04 @DB
  Scenario: There are multiple RNA family types
    When db query:
      """sql
      SELECT DISTINCT type FROM family
      """
    Then db query count > 5