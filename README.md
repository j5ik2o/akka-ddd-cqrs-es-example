# Bank API CQRS Event Sourcing System on Akka-Cluster (Concept Model)

## Features

- DDD, CQRS + Event Sourcing based design
- Implement using Akka(-actor, -stream, -cluster-sharding, -persistence, -persistence-query, -http, ...)
- Scala 2.12.4
- REST API Application

## Target domain

- Bank Account to deposit/withdraw

## Target Use cases

- Deposit money to a Bank Account
- Withdraw money from a Bank Account
- Refer to deposits and withdraws in a Bank Account

## Layered structure

In this project, layered structure is based on 'Clean architecture'.

### Domain layer

- Domain objects is represented by case class.
- Domain types
    - [BankAccountId](domain/src/main/scala/com/github/j5ik2o/bank/domain/model/BankAccountId.scala)
    - [BankAccount](domain/src/main/scala/com/github/j5ik2o/bank/domain/model/BankAccount.scala)
    - [BankAccountEventId](domain/src/main/scala/com/github/j5ik2o/bank/domain/model/BankAccountEventId.scala)
    - [BankAccountEvent](domain/src/main/scala/com/github/j5ik2o/bank/domain/model/BankAccountEvent.scala)
        - BankAccountOpened is the account opening event
        - BankAccountUpdated is the account information updating event
        - BankAccountDeposited is the deposit event
        - BankAccountWithdrawn is the withdarw event
        - BankAccountClosed is the account closed event

### Use case layer

- Command use case is [BankAccountAggregateUseCase](use-case/src/main/scala/com/github/j5ik2o/bank/useCase/BankAccountAggregateUseCase.scala)
- Query use case is [BankAccountReadModelUseCase](use-case/src/main/scala/com/github/j5ik2o/bank/useCase/BankAccountReadModelUseCase.scala)

### Interface layer

#### Aggregate

- BankAccountAggregate(PersistentActor)
- ShardedBankAccountAggregate(BankAccountAggregate for cluster-sharding)
- ShardedBankAccountAggregates(proxy to ShardRegion)

#### Controller

- BankAccountController

#### Persistence

- Slick3
    - Daos
        - BankAccountDao
        - BankAccountEventDao
    - Records
        - BankAccountRecord
        - BankAccountEventRecord

## How to run unit tests

```sh
$ sbt clean test
```

## How to run E2E tests

Terminal #1

```sh
$ sbt -DPORT=2551 -DHTTP_PORT=8080 'localMysql/run' 'api-server/run'
```

Terminal #2

```sh
$ sbt -DPORT=2552 -DHTTP_PORT=8081 'api-server/run'
```

Terminal #3

```sh
$ sbt -DPORT=2553 -DHTTP_PORT=8082 'api-server/run'
```

Terminal #4

```sh
$ sbt 'read-model-updater/run'
```

## How to test

```sh
$ curl -X POST \
  http://localhost:$PORT/bank-accounts \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{ "name": "test-1" }'
{"id":"XEe","errorMessage":null}%

$ curl -X PUT \
  http://localhost:8080/bank-accounts/XEe \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
  "name": "test-2"
}'
{"id":"XEe","errorMessage":null}%

$ curl -X PUT \
  http://localhost:8080/bank-accounts/XEe/events \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
	"type": "deposit",
	"amount": 1000,
	"currencyCode": "JPY"
}'
{"id":"XEe","errorMessage":null}%

$ curl -X PUT \
  http://localhost:8080/bank-accounts/XEe/events \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
	"type": "withdraw",
	"amount": 500,
	"currencyCode": "JPY"
}'
{"id":"XEe","errorMessage":null}%

$ curl -X GET \
  http://localhost:8080/bank-accounts/XEe \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json'
{
    "id": "XEe",
    "values": [
        {
            "type": "deposit",
            "amount": 1000,
            "currencyCode": "JPY",
            "createAt": 1520219459
        }
    ],
    "errorMessage": null
}

$ curl -X DELETE \
  http://localhost:8080/bank-accounts/XEe \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json'
{
    "id": "XEe",
    "errorMessage": null
}
{"id":"XEe","errorMessage":null}%
```
