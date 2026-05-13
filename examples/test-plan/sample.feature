Feature: Posts REST API
  This feature demonstrates all available REST plugin steps
  against the JSONPlaceholder public API (https://jsonplaceholder.typicode.com).

  # -------------------------------------------------------------
@ID-1 @GET
  Scenario: List all posts
    When I make a GET request to "posts"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      [{"userId": 1, "id": 1}]
      """

    # -------------------------------------------------------------
  @ID-2 @GET
  Scenario: Get a single post
    When I make a GET request to "posts/1"
    Then the HTTP status code is equal to 200
    And the response body is:
      """json
      {
        "userId": 1,
        "id": 1,
        "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
        "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum est autem sunt rem eveniet architecto"
      }
      """

    # -------------------------------------------------------------
  @ID-3 @POST
  Scenario: Create a post with inline body
    When I make a POST request to "posts" with body:
      """json
      {
        "title": "My new post",
        "body": "Content of my new post.",
        "userId": 1
      }
      """
    Then the HTTP status code is equal to 201
    And the response body contains:
      """json
      {"title": "My new post", "userId": 1}
      """

    # -------------------------------------------------------------
  @ID-4 @POST
  Scenario: Create a post with body from file
    When I make a POST request to "posts" with body from file "fixtures/new-post.json"
    Then the HTTP status code is equal to 201
    And the response body contains:
      """json
      {"title": "Post from file", "userId": 1}
      """

    # -------------------------------------------------------------
  @ID-5 @PUT
  Scenario: Replace a post
    When I make a PUT request to "posts/1" with body:
      """json
      {
        "id": 1,
        "title": "Replaced title",
        "body": "Replaced body content.",
        "userId": 1
      }
      """
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {"title": "Replaced title"}
      """

    # -------------------------------------------------------------
  @ID-6 @PATCH
  Scenario: Partially update a post
    When I make a PATCH request to "posts/1" with body:
      """json
      {"title": "Patched title"}
      """
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {"id": 1, "title": "Patched title"}
      """

    # -------------------------------------------------------------
  @ID-7 @POST
  Scenario: Delete a post
    When I make a DELETE request to "posts/1"
    Then the HTTP status code is equal to 200

    # -------------------------------------------------------------
  @ID-8 @GET
  Scenario: Get a non-existent post returns 404
    When I make a GET request to "posts/9999"
    Then the HTTP status code is equal to 404

    # -------------------------------------------------------------
  @ID-9 @POST
  Scenario: Trigger an action with no body
    When I make a POST request to "posts/1/comments"
    Then the HTTP status code is not equal to 500

    # -------------------------------------------------------------
  @ID-10 @POST @GET
  Scenario: Create a post and get it after
    When I make a POST request to "posts" with body:
      """json
      {
          "title": "My new post",
          "body": "Content of my new post.",
          "userId": 1
      }
      """
    Then I store the value of field 'id' from the response body into variable id
    Then I make a GET request to 'posts/${id}'
    Then the HTTP status code is 200

    # -------------------------------------------------------------
  @ID-11 @GET
  Scenario: Measure response time
    Given benchmark mode is enabled with 200 executions and 10 threads
    When I make a GET request to "posts/1"
    Then the benchmark P95 response time (ms) is greater than 2

  @ID-12 @GET
  Scenario: List all posts (2)
    When I make a GET request to "posts"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      [{"userId": 1, "id": 1}]
      """
