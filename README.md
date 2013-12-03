# Sample Scala Event Producer for Amazon Kinesis

## Introduction

This is an example event producer for [Amazon Kinesis] [kinesis] written in Scala and packaged as an SBT project.

This was built by the [Snowplow Analytics] [snowplow] team, as part of a proof of concept for porting our event collection and enrichment processes to run on Kinesis.

**Please note:** Amazon Kinesis is currently in private beta. Being on the Kinesis private beta is a pre-requisite to building and running this project.

## Building

Assuming you already have [SBT 0.13.0] [sbt] installed:

    $ git clone git://github.com/snowplow/kinesis-example-scala-producer.git
    
Now manually copy the relevant jar from your Amazon Kinesis SDK Preview:

    $ cd kinesis-example-scala-producer
    $ cp ~/downloads/AmazonKinesisSDK-preview/aws-java-sdk-1.6.4/lib/aws-java-sdk-1.6.4.jar lib/
    $ sbt assembly

The 'fat jar' is now available as:

    target/scala-2.10/kinesis-example-scala-producer-assembly-0.0.1.jar

## Unit testing

To come.

## Usage

The event producer has the following command-line interface:

```
kinesis-example-scala-producer: Version 0.0.1. Copyright (c) 2013, Snowplow
Analytics Ltd.

Usage: kinesis-example-scala-producer [OPTIONS]

OPTIONS
--config filename  Configuration file. Defaults to "resources/default.conf"
                   (within .jar) if not set
--create           Create the stream before producing events
```

## Running

Create your own config file:

    $ cp src/main/resources/default.conf my.conf

Now edit it and update the AWS credentials:

```js
aws {
  access-key: "cpf"
  secret-key: "cpf"
}
```

You can leave the rest of the settings for now.

Next, run the event producer, making sure to specify your new config file and create a new stream:

    $ java -jar target/scala-2.10/kinesis-example-scala-producer-assembly-0.0.1.jar --config ./my.conf --create 

Finally, verify that events are being sent to your stream in the [Kinesis Management Console] [kinesis-ui]:

![ui-screengrab] [ui-screengrab]

## Next steps

Fork this project and adapt it into your own custom Kinesis event producer.

## FAQ

**Why isn't aws-java-sdk-1.6.4.jar included in this repo?**

This file is only available to those on the Amazon Kinesis private beta.

**Is a Kinesis event producer the right place to put stream setup code?**

Probably not - best practice would be to handle this as part of your standard AWS devops flow, assigning appropriately-locked down IAM permissions etc. However, this stream setup functionality is included in this project, to simplify getting started.

**What about an example Kinesis event consumer aka "Kinesis application" in Scala?**

We are working on one! Stay tuned.

## Roadmap

1. Add support for ordered events in the stream [[#1]](#1)
2. Add logging output to the producer [[#2]](#2)
3. Add live Kinesis integration tests to the test suite [[#3]](#3)
4. Add ability to send events as Thrift [[#4]](#4) or Avro [[#5]](#5)
5. Rebase to run on top of Akka (maybe) [[#6]](#6)

If you would like to help with any of these, feel free to submit a pull request.

## Copyright and license

Copyright 2013 Snowplow Analytics Ltd, with portions copyright
2013 Amazon.com, Inc or its affiliates.

Licensed under the [Apache License, Version 2.0] [license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[kinesis]: http://aws.amazon.com/kinesis/
[snowplow]: http://snowplowanalytics.com
[sbt]: http://typesafe.artifactoryonline.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.0/sbt-launch.jar

[kinesis-ui]: https://console.aws.amazon.com/kinesis/?
[ui-screengrab]: misc/kinesis-stream-summary.png

[license]: http://www.apache.org/licenses/LICENSE-2.0