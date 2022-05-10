# Openmoneymarket (OMM) Java Contracts

![Gradle](https://img.shields.io/badge/gradle-7.2-blue)

This repository contains the smart contracts for OMM in Java.

### Requirement

- JDK 11+

### How to run integration test

- create/update gradle.properties with following configuration

```
score-test.url= https://sejong.net.solidwallet.io/api/v3 or http://localhost:9082/api/v3
score-test.nid= 0x53 or 0x3
score-test.keystoreName= path to deployer wallet
score-test.keystorePass= deployer wallet password
score-test.tester.keystoreName= secondary wallet path
score-test.tester.keystorePass= secondary wallet password
```

- execute following command

```./gradlew <Module name>:integrationTest```

```./gradlew :core-contracts:RewardDistribution:integrationTest```