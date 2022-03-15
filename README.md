# openmoneymarket Java contracts


### build all scores:

`./gradlew clean build optimizedJar`

### build and deploy a particular score

`./gradlew clean build dao-fund:optimizedJar`
`./gradlew dao-fund:deployToLocal`


You should already have a wallet created and a goloop node running locally


### run integration tests
`./gradlew clean build optimizedJar dao-fund:itest --info`
