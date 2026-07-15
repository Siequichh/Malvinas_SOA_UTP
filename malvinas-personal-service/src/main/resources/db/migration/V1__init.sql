-- Malvinas personal-service — schema staff
CREATE SCHEMA IF NOT EXISTS staff;

CREATE TABLE IF NOT EXISTS staff.roles (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(3)   NOT NULL UNIQUE,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    modified_at TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255),
    modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS staff.employees (
    id               SERIAL PRIMARY KEY,
    dni              VARCHAR(8)   NOT NULL UNIQUE,
    email            VARCHAR(150) UNIQUE,
    first_name       VARCHAR(255),
    last_name        VARCHAR(255),
    phone            VARCHAR(255),
    password_hash    VARCHAR(255),
    hire_date        DATE,
    license_number   VARCHAR(255),
    license_category VARCHAR(255),
    is_active        BOOLEAN  NOT NULL DEFAULT TRUE,
    role_id          INTEGER  NOT NULL REFERENCES staff.roles(id),
    created_at       TIMESTAMP NOT NULL,
    modified_at      TIMESTAMP NOT NULL,
    created_by       VARCHAR(255),
    modified_by      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS staff.attendances (
    id             SERIAL PRIMARY KEY,
    employee_id    INTEGER  NOT NULL REFERENCES staff.employees(id),
    date           DATE     NOT NULL,
    status         VARCHAR(2),
    check_in_time  TIME,
    check_out_time TIME,
    remark         VARCHAR(200),
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL,
    created_by     VARCHAR(255),
    modified_by    VARCHAR(255),
    UNIQUE (employee_id, date)
);

CREATE TABLE IF NOT EXISTS staff.refresh_tokens (
    id          SERIAL PRIMARY KEY,
    employee_id INTEGER      NOT NULL REFERENCES staff.employees(id),
    token       VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(200),
    ip_address  VARCHAR(50),
    expires_at  TIMESTAMP    NOT NULL,
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL,
    modified_at TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255),
    modified_by VARCHAR(255)
);
