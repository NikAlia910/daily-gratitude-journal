Feature: Daily Gratitude Journaling and Reflection

  As a mindful individual seeking personal growth and emotional well-being
  I want to be able to easily record and reflect on my daily gratitude
  So that I can cultivate a habit of gratitude and build emotional resilience

  Background:
    Given I am an authenticated user

  # Acceptance Criteria 1: I can open the app and easily access a screen to write a new gratitude entry for the current day
  Scenario: Create a new gratitude entry for today
    When I create a new gratitude entry for today
    And I write "I am grateful for my family's health and support during challenging times"
    Then the gratitude entry should be saved successfully
    And the entry should be associated with today's date

  # Acceptance Criteria 2: I can add a mood emoji or tag to my gratitude entry
  Scenario: Add mood to gratitude entry
    When I create a new gratitude entry for today
    And I write "I am grateful for the beautiful sunrise this morning"
    And I select the mood "GRATEFUL"
    Then the gratitude entry should be saved with the selected mood

  Scenario: Create gratitude entry with different moods
    When I create a gratitude entry with mood "HAPPY" and text "I got promoted at work today!"
    Then the entry should be saved with mood "HAPPY"
    When I create a gratitude entry with mood "REFLECTIVE" and text "Thinking about life's simple pleasures"
    Then the entry should be saved with mood "REFLECTIVE"

  # Acceptance Criteria 3: My gratitude entry is automatically timestamped and saved
  Scenario: Gratitude entry is automatically timestamped
    When I create a new gratitude entry for today
    And I write "I am thankful for good friends who listen"
    Then the entry should be automatically timestamped
    And the timestamp should be within the last few seconds

  # Acceptance Criteria 4: I can view a list or calendar of my past gratitude entries
  Scenario: View past gratitude entries
    Given I have created gratitude entries on previous days
    When I request to view my past gratitude entries
    Then I should see a list of my previous entries
    And each entry should show the date, text, and mood if selected

  Scenario: View gratitude entries by date
    Given I created a gratitude entry on "2024-01-01" with text "New Year gratitude"
    And I created a gratitude entry on "2024-01-02" with text "Another day of blessings"
    When I request entries for a specific date range
    Then I should see entries organized by date
    And the most recent entries should appear first

  # Acceptance Criteria 5: I can edit or delete my past gratitude entries
  Scenario: Edit existing gratitude entry
    Given I have a gratitude entry with text "I am grateful for coffee"
    When I edit the entry to "I am grateful for morning coffee and quiet moments"
    And I change the mood to "CONTENT"
    Then the entry should be updated with the new text and mood
    And the original timestamp should be preserved

  Scenario: Delete gratitude entry
    Given I have a gratitude entry with text "This entry will be deleted"
    When I delete the gratitude entry
    Then the entry should no longer exist in my journal
    And I should not see it in my past entries list

  # Business Logic: Each entry is associated with a specific date
  Scenario: Cannot create multiple entries for the same date
    Given I have already created a gratitude entry for today
    When I try to create another gratitude entry for the same date
    Then the system should prevent creating duplicate entries for the same date

  # Business Logic: Handle edge cases
  Scenario: Create entry with minimal required information
    When I create a gratitude entry with only the required text "Grateful for health"
    Then the entry should be saved successfully
    And it should have today's date and current timestamp
    And the mood should be optional/null

  # Additional functionality: Entries are private to the user
  Scenario: User can only see their own entries
    Given another user has created gratitude entries
    When I request to view gratitude entries
    Then I should only see my own entries
    And I should not see other users' entries

  # Data validation scenarios
  Scenario: Cannot create entry with empty text
    When I try to create a gratitude entry with empty text
    Then the entry should not be saved
    And I should receive a validation error

  Scenario: Entry text can be long
    When I create a gratitude entry with a very long text of 1000 characters
    Then the entry should be saved successfully
    And all text should be preserved 