# Bank API CQRS Event Sourcing System on Akka-Cluster (Concept Model)

## Features

- DDD, CQRS + Event Sourcing based design
- Implement using Akka(-actor, -stream, -cluster-sharding, -persistence, -persistence-query, -http, ...)
- Scala 2.12.4
- REST API Application

## Target domain

- Bank Account to deposit/withdraw

## Use case

- Deposit a money to a Bank Account
- Withdraw a money from a Bank Account
- Refer to deposits and withdraws in a Bank Account

## Layered structure

In this project, layered structure is based on 'Clean architecture'.

### Domain layer

- Domain objects is represented by case class.
- Domain types
    - BankAccountId
    - BankAccount
    - BankAccountEventId
    - BankAccountEvent
        - BankAccountOpened
        - BankAccountUpdated
        - BankAccountDeposited
        - BankAccountWithdrawn
        - BankAccountClosed

### Use case layer

- Command use case is BankAccountAggregateUseCase
- Query use case is BankAccountReadModelUseCase

### Interface layer

#### Aggregate

- BankAccountAggregate(PersistentActor)
- ShardedBankAccountAggregate(BankAccountAggregate for cluster-sharding)
- ShardedBankAccountAggregates(proxy to ShardRegion)

#### Controller

- BankAccountController

#### Persistence

- Slick3 Daos
    - BankAccountDao
    - BankAccountEventDao


## How to run

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
