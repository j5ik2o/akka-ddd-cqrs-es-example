# CREATE SCHEMA IF NOT EXISTS "public";

CREATE TABLE journal (
  ordering        SERIAL,
  persistence_id  VARCHAR(255) NOT NULL,
  sequence_number BIGINT       NOT NULL,
  deleted         BOOLEAN      DEFAULT FALSE,
  tags            VARCHAR(255) DEFAULT NULL,
  message         BLOB         NOT NULL,
  PRIMARY KEY (persistence_id, sequence_number)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE UNIQUE INDEX journal_ordering_idx
  ON journal (ordering);

CREATE TABLE snapshot (
  persistence_id  VARCHAR(255) NOT NULL,
  sequence_number BIGINT       NOT NULL,
  created         BIGINT       NOT NULL,
  snapshot        BLOB         NOT NULL,
  PRIMARY KEY (persistence_id, sequence_number)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

--

CREATE TABLE `bank_account_id_sequence_number`(id bigint unsigned NOT NULL) ENGINE=MyISAM;
INSERT INTO `bank_account_id_sequence_number` VALUES (100);

CREATE TABLE `bank_account_event_id_sequence_number`(id bigint unsigned NOT NULL) ENGINE=MyISAM;
INSERT INTO `bank_account_event_id_sequence_number` VALUES (100);

CREATE TABLE `bank_account` (
  `id`          BIGINT                      NOT NULL,
  `deleted`     BOOLEAN                     NOT NULL,
  `name`        VARCHAR(64)                 NOT NULL,
  `sequence_nr` BIGINT                      NOT NULL,
  `created_at`  DATETIME(6)                 NOT NULL,
  `updated_at`  DATETIME(6)                 NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE `bank_account_event` (
  `id`              BIGINT                       NOT NULL,
  `bank_account_id` BIGINT                       NOT NULL,
  `type`            ENUM ('deposit', 'withdraw') NOT NULL,
  `amount`          BIGINT                       NOT NULL,
  `currency_code`   VARCHAR(64)                  NOT NULL,
  `created_at`      DATETIME(6)                  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `bank_account_event_bank_account_id_idx` (`bank_account_id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;





