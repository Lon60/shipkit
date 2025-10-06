BEGIN;

create table deployment
(
    id         uuid                        not null
        primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table project
(
    id         uuid                        not null
        primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table template
(
    id         uuid                        not null
        primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table users
(
    id         uuid                        not null
        primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

COMMIT;