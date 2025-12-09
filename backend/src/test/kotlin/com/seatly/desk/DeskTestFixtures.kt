package com.seatly.desk

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

fun createDesk(
  client: HttpClient,
  authToken: String,
  name: String,
  location: String,
): DeskResponse {
  val createRequest =
    CreateDeskRequest(
      name = name,
      location = location,
    )

  val response =
    client.toBlocking().exchange(
      HttpRequest
        .POST("/desks", createRequest)
        .bearerAuth(authToken),
      DeskResponse::class.java,
    )

  assertEquals(HttpStatus.CREATED, response.status)
  val body = response.body()
  assertNotNull(body)

  return body!!
}
