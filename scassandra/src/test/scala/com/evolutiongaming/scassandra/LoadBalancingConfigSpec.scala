package com.evolutiongaming.scassandra

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}

class LoadBalancingConfigSpec extends FunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    LoadBalancingConfig(config) shouldEqual LoadBalancingConfig.Default
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("load-balancing.conf"))
    val expected = LoadBalancingConfig(
      localDc = "local",
      allowRemoteDcsForLocalConsistencyLevel = true)
    LoadBalancingConfig(config) shouldEqual expected
  }
}