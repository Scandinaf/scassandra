package com.evolutiongaming.scassandra

import com.datastax.driver.core.{HostDistance, PoolingOptions => PoolingOptionsJ}
import com.evolutiongaming.config.ConfigHelper._
import com.typesafe.config.Config

import scala.concurrent.duration._

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/pooling/]]
  */
final case class PoolingConfig(
  local: PoolingConfig.HostConfig = PoolingConfig.HostConfig.Local,
  remote: PoolingConfig.HostConfig = PoolingConfig.HostConfig.Remote,
  poolTimeout: FiniteDuration = 5.seconds,
  idleTimeout: FiniteDuration = 2.minutes,
  maxQueueSize: Int = 256,
  heartbeatInterval: FiniteDuration = 30.seconds) {

  import PoolingConfig._

  def asJava: PoolingOptionsJ = {
    new PoolingOptionsJ()
      .set(local, HostDistance.LOCAL)
      .set(remote, HostDistance.REMOTE)
      .setPoolTimeoutMillis(poolTimeout.toMillis.toInt)
      .setIdleTimeoutSeconds(idleTimeout.toSeconds.toInt)
      .setMaxQueueSize(maxQueueSize)
      .setHeartbeatIntervalSeconds(heartbeatInterval.toSeconds.toInt)
  }
}

object PoolingConfig {

  val Default: PoolingConfig = PoolingConfig()


  def apply(config: Config): PoolingConfig = apply(config, Default)

  def apply(config: Config, default: => PoolingConfig): PoolingConfig = {

    def group(name: String, default: HostConfig) = {
      config.getOpt[Config](name).fold(default) { config => HostConfig(config, default) }
    }

    def get[A: FromConf](name: String) = config.getOpt[A](name)

    PoolingConfig(
      local = group("local", default.local),
      remote = group("remote", default.remote),
      poolTimeout = get[FiniteDuration]("pool-timeout") getOrElse default.poolTimeout,
      idleTimeout = get[FiniteDuration]("idle-timeout") getOrElse default.idleTimeout,
      maxQueueSize = get[Int]("max-queue-size") getOrElse default.maxQueueSize,
      heartbeatInterval = get[FiniteDuration]("heartbeat-interval") getOrElse default.heartbeatInterval)
  }


  final case class HostConfig(
    newConnectionThreshold: Int,
    maxRequestsPerConnection: Int,
    connectionsPerHostMin: Int,
    connectionsPerHostMax: Int)

  object HostConfig {

    val Local: HostConfig = HostConfig(
      newConnectionThreshold = 800,
      maxRequestsPerConnection = 32768,
      connectionsPerHostMin = 1,
      connectionsPerHostMax = 4)

    val Remote: HostConfig = HostConfig(
      newConnectionThreshold = 200,
      maxRequestsPerConnection = 2000,
      connectionsPerHostMin = 1,
      connectionsPerHostMax = 4)

    
    def apply(config: Config, default: => HostConfig): HostConfig = {
      HostConfig(
        newConnectionThreshold = config.getOpt[Int]("new-connection-threshold") getOrElse default.newConnectionThreshold,
        maxRequestsPerConnection = config.getOpt[Int]("max-requests-per-connection") getOrElse default.maxRequestsPerConnection,
        connectionsPerHostMin = config.getOpt[Int]("connections-per-host-min") getOrElse default.connectionsPerHostMin,
        connectionsPerHostMax = config.getOpt[Int]("connections-per-host-max") getOrElse default.connectionsPerHostMax)
    }
  }


  implicit class PoolingOptionsJOps(val self: PoolingOptionsJ) extends AnyVal {

    def set(hostConfig: HostConfig, distance: HostDistance): PoolingOptionsJ = {
      self
        .setNewConnectionThreshold(distance, hostConfig.newConnectionThreshold)
        .setMaxRequestsPerConnection(distance, hostConfig.maxRequestsPerConnection)
        .setConnectionsPerHost(distance, hostConfig.connectionsPerHostMin, hostConfig.connectionsPerHostMax)
    }
  }
}
