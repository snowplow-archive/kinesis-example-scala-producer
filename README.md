# Sample Scala Event Producer for Amazon Kinesis

## Introduction

This is an example event producer for [Amazon Kinesis] [kinesis] written in
Scala and packaged as an SBT project.

This was built by the [Snowplow Analytics] [snowplow] team, as part of a
proof of concept for porting our event collection and enrichment processes
to run on Kinesis.

This has been built to run in conjunction with the
[kinesis-example-scala-consumer] [consumer].

## Pre-requisites

This project requires Java 7 or 8, SBT 0.13.0 and Thrift:

* On Mac OS X, Thrift is easily installed with `brew install thrift`.
* On Linux, `sudo apt-get install thrift-compiler libthrift-java`.

All dependencies are handled for you if you use Vagrant (see next section).

## Building

Assuming git, [Vagrant] [vagrant-install] and [VirtualBox] [virtualbox-install] installed:

```bash
 host> git clone https://github.com/snowplow/kinesis-example-scala-producer
 host> cd kinesis-example-scala-producer
 host> vagrant up && vagrant ssh
guest> cd /vagrant
guest> sbt compile
```

## Unit testing

To come.

## Usage

The event producer has the following command-line interface:

```
kinesis-example-scala-producer: Version 0.1.2. Copyright (c) 2013, Snowplow
Analytics Ltd.

Usage: kinesis-example-scala-producer [OPTIONS]

OPTIONS
--config filename  Configuration file. Defaults to "resources/default.conf"
                   (within .jar) if not set
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

Make sure that the AWS credentials you use have the permissions required to:

1. Create and write to the Kinesis stream specified in the config file
2. Create tables in DynamoDB

You can leave the rest of the settings for now.

Next, run the event producer, making sure to specify your new config file.
The stream will be created if it doesn't already exist.

    $ sbt "run --config ./my.conf"

Finally, verify that events are being sent to your stream by using
[snowplow/kinesis-example-scala-consumer] [kinesis-consumer] or checking
the [Kinesis Management Console] [kinesis-ui]:

![ui-screengrab] [ui-screengrab]

## Next steps

Fork this project and adapt it into your own custom Kinesis event producer.

## FAQ

**Is a Kinesis event producer the right place to put stream setup code?**

Probably not - best practice would be to handle this as part of your standard AWS devops flow, assigning appropriately-locked down IAM permissions etc. However, this stream setup functionality is included in this project, to simplify getting started.

**What about an example Kinesis event consumer aka "Kinesis application" in Scala?**

See [snowplow/kinesis-example-scala-consumer] [consumer].

## Roadmap

1. Add support for ordered events in the stream [[#1]](#1)
2. Add logging output to the producer [[#2]](#2)
3. Add live Kinesis integration tests to the test suite [[#3]](#3)
4. Add ability to send events as Avro [[#5]](#5)
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
[consumer]: https://github.com/snowplow/kinesis-example-scala-consumer

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[dev-environment]: https://github.com/snowplow/dev-environment
[dev-env-readme]: https://github.com/snowplow/dev-environment/blob/master/README.md
[playbook]: https://github.com/snowplow/ansible-playbooks/blob/master/snowplow-batch-pipeline.yml

[kinesis-consumer]: https://github.com/snowplow/kinesis-example-scala-consumer
[kinesis-ui]: https://console.aws.amazon.com/kinesis/?
[ui-screengrab]: misc/kinesis-stream-summary.png

[license]: http://www.apache.org/licenses/LICENSE-2.0
