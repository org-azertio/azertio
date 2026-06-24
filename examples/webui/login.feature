# language: dsl
#
# Target: https://the-internet.herokuapp.com/login
# Credentials: tomsmith / SuperSecretPassword!
#
# Demonstrates: navigate, fill, click, assert URL, assert visible, assert text
Feature: Login

  Scenario: Successful login redirects to the secure area
    * navigate to web page "https://the-internet.herokuapp.com/login"
    * assert web page title = "The Internet"
    * fill UI "#username" with "tomsmith"
    * fill UI "#password" with "SuperSecretPassword!"
    * click UI "button[type='submit']"
    * assert web page URL has "/secure"
    * assert UI "#flash" is visible
    * assert UI "#flash" text has "You logged into a secure area!"

  Scenario: Wrong credentials show an error message
    * navigate to web page "https://the-internet.herokuapp.com/login"
    * fill UI "#username" with "wrong"
    * fill UI "#password" with "wrong"
    * click UI "button[type='submit']"
    * assert web page URL has "/login"
    * assert UI "#flash" is visible
    * assert UI "#flash" text has "Your username is invalid!"
