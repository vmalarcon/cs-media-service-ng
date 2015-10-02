package com.expedia.www.cs.media.controller

import org.scalatest.{GivenWhenThen, FunSpec}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.EasyMockSugar
import org.springframework.boot.test.{IntegrationTest, SpringApplicationConfiguration}
import org.springframework.http.ResponseEntity

class ServiceControllerSpec extends FunSpec with GivenWhenThen with ShouldMatchers with EasyMockSugar {

  describe("A valid instance of ServiceController") {
    it ("should return string 'Hello there!'") {
      Given("a valid ServiceController")
      val serviceController = new ServiceController
      val msg = "Hello there!"
      serviceController.setMessage(msg)
      When("hello is invoked")
      val actual = serviceController.hello
      Then("the result should match the pre-set value")
      def body: HelloMessage = actual.getBody.asInstanceOf[HelloMessage]
      body.getMessage should equal(msg)
    }
  }
}
