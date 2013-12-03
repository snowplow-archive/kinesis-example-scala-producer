/*
 * Copyright (c) 2013 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.kinesis.producer

// Java
import java.nio.ByteBuffer

// Amazon
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.{
  BasicAWSCredentials,
  ClasspathPropertiesFileCredentialsProvider
}

// Kinesis
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.{
  CreateStreamRequest,
  DescribeStreamRequest,
  PutRecordRequest
}

// Config
import com.typesafe.config.Config

// SnowPlow Utils
import com.snowplowanalytics.util.Tap._

/**
 * The core logic for the Kinesis event producer
 */
case class StreamProducer(config: Config) {

  // Grab all the configuration variables one-time
  private object ProducerConfig {

    private val producer = config.getConfig("producer")

    private val aws = producer.getConfig("aws")
    val awsAccessKey = aws.getString("access-key")
    val awsSecretKey = aws.getString("secret-key")

    private val stream = producer.getConfig("stream")
    val streamName = stream.getString("name")
    val streamSize = stream.getInt("size")

    private val events = producer.getConfig("events")
    val eventsOrdered = events.getBoolean("ordered")
    val eventsLimit = {
      val l = events.getInt("limit")
      if (l == 0) None else Some(l)
    }

    private val ap = producer.getConfig("active-polling")
    val apDuration = ap.getInt("duration")
    val apInterval = ap.getInt("interval")
  }

  // Initialize
  private val kinesis = createKinesisClient(ProducerConfig.awsAccessKey, ProducerConfig.awsSecretKey)

  /**
   * Creates a new stream. Arguments are
   * optional - defaults to the values
   * provided in the ProducerConfig if
   * not provided.
   *
   * @param name The name of the stream
   * to create
   * @param size The number of shards to
   * support for this stream
   * @param duration How long to keep
   * checking if the stream became active,
   * in seconds
   * @param interval How frequently to
   * check if the stream has become active,
   * in seconds
   *
   * @return an Option-boxed Boolean, where:
   * 1. None means we did not wait to see if
   *    the stream became active
   * 2. Some(true) means the stream became active
   *    while we were polling its status
   * 3. Some(false) means the stream did not
   *    become active while we were polling 
   */
  def createStream(name: String = ProducerConfig.streamName, size: Int = ProducerConfig.streamSize, duration: Int = ProducerConfig.apDuration, interval: Int = ProducerConfig.apInterval): Option[Boolean] = {

    val createReq = new CreateStreamRequest().tap { r =>
      r.setStreamName(name)
      r.setShardCount(size)
    }

    kinesis.createStream(createReq)
    waitForStream(name, duration, interval)
  }

  /**
   * Produces an (in)finite stream of events.
   *
   * @param name The name of the stream
   * to produce events for
   * @param ordered Whether the sequence
   * numbers of the events should always be
   * ordered
   * @param limit How many events to produce
   * in this stream. Use None for an infinite
   * stream
   */
  def produceStream(name: String = ProducerConfig.streamName, ordered: Boolean = ProducerConfig.eventsOrdered, limit: Option[Int] = ProducerConfig.eventsLimit) {
    
    def write() = writeExampleRecord(name, System.currentTimeMillis()) // Alias
    (ordered, limit) match {
      case (false, None)    => while (true) { write() }
      case (true,  None)    => throw new RuntimeException("Ordered stream support not yet implemented") // TODO
      case (false, Some(c)) => (1 to c).foreach(_ => write())
      case (true,  Some(c)) => throw new RuntimeException("Ordered stream support not yet implemented") // TODO
    }
  }

  /**
   * Creates a new Kinesis client from
   * provided AWS access key and secret
   * key. If both are set to "cpf", then
   * authenticate using the classpath
   * properties file.
   *
   * @return the initialized
   * AmazonKinesisClient
   */
  private[producer] def createKinesisClient(accessKey: String, secretKey: String): AmazonKinesisClient =
    if (isCpf(accessKey) && isCpf(secretKey)) {
      new AmazonKinesisClient(new ClasspathPropertiesFileCredentialsProvider())  
    } else if (isCpf(accessKey) || isCpf(secretKey)) {
      throw new RuntimeException("access-key and secret-key must both be set to 'cpf', or neither of them")
    } else {
      new AmazonKinesisClient(new BasicAWSCredentials(accessKey, secretKey))
    }

  /**
   * Waits until a newly created stream
   * has status ACTIVE and thus is ready
   * to send events to.
   *
   * @param name The name of the stream
   * to wait for
   * @param duration How long to keep
   * checking if the stream became active,
   * in seconds
   * @param interval How frequently to
   * check if the stream has become active,
   * in seconds
   *
   * @return an Option-boxed Boolean, where:
   * 1. None means we did not wait to see if
   *    the stream became active
   * 2. Some(true) means the stream became active
   *    while we were polling its status
   * 3. Some(false) means the stream did not
   *    become active while we were polling 
   */
  private[producer] def waitForStream(name: String, duration: Int, interval: Int): Option[Boolean] = {

    if (duration < 1) {
      return None // Not waiting for stream
    }

    val endTime = System.currentTimeMillis() + (duration * 1000)
    while (System.currentTimeMillis() < endTime) {
      safeSleep(interval)
      if (getStreamStatus(name) == Some("ACTIVE")) {
        return Some(true) // Went active
      }
    }

    Some(false) // Never went active
  }

  /**
   * Safe sleep routine
   *
   * @param duration How long to sleep for,
   * in seconds
   */
  private[producer] def safeSleep(duration: Int) {
    try {
      Thread.sleep(duration * 1000)
    } catch {
      case e: InterruptedException => // Ignore interruption
    }
  }

  /**
   * Helper to get the current status
   * of a Kinesis stream
   *
   * @param name The name of the stream
   * to get the status of
   *
   * @return an Option boxing either the
   * stream's status, or None if the
   * resource doesn't exist yet
   */
  private[producer] def getStreamStatus(name: String): Option[String] = {
    
    val req = new DescribeStreamRequest().tap { r =>
      r.setStreamName(name)
      r.setLimit(1) // Not interested in the shard records
    }
    
    try {
      val res = kinesis.describeStream(req)
      Some(res.getStreamDescription.getStreamStatus)
    } catch {
      case ase: AmazonServiceException if isResourceNotFoundException(ase) =>
        None // Stream doesn't exist yet
    }
  }

  /**
   * Writes an example record to the given
   * stream. Uses the supplied timestamp
   * to make the record identifiable.
   *
   * @param stream The name of the stream
   * to write the record to
   * @param timestamp When this record was
   * created
   *
   * @return the shard ID this record was
   * written to
   */
  private[producer] def writeExampleRecord(stream: String, timestamp: Long): String =
    writeRecord(stream,
      data = "example-record-%s".format(timestamp),
      key = "partition-key-%s".format(timestamp % 100000)
    )

  /**
   * Writes a record to the given stream
   *
   * @param stream The name of the stream
   * to write the record to
   * @param data The data for this record
   * @param key The partition key for this
   * record
   *
   * @return the shard ID this record was
   * written to
   */
  private[producer] def writeRecord(stream: String, data: String, key: String): String = {

    val req = new PutRecordRequest().tap { r => 
      r.setStreamName(stream)
      r.setData(ByteBuffer.wrap(data.getBytes))
      r.setPartitionKey(key)
    }
    
    val res = kinesis.putRecord(req)
    res.getShardId
  }

  /**
   * Is the access/secret key set to
   * the special value "cpf" i.e. use
   * the classpath properties file
   * for credentials.
   *
   * @param key The key to check
   *
   * @return true if key is cpf, false
   * otherwise
   */
  private[producer] def isCpf(key: String): Boolean =
    (key == "cpf")

  /**
   * Is this exception a ResourceNotFoundException?
   *
   * @param ase The AmazonServiceException to check
   *
   * @return true if it's a ResourceNotFoundException,
   * false otherwise
   */
  private[producer] def isResourceNotFoundException(ase: AmazonServiceException): Boolean = 
    ase.getErrorCode.equalsIgnoreCase("ResourceNotFoundException")
}
