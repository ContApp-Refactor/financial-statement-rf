create table financial_statement (
    id bigserial primary key,
    report_id uuid not null unique,
    type varchar(64) not null,
    ent_id varchar(255) not null,
    created_at timestamp with time zone not null,
    report_snapshot jsonb not null
);

create unique index idx_financial_statement_report_id on financial_statement (report_id);
create index idx_financial_statement_ent_id on financial_statement (ent_id);

create table financial_statement_email_schedule (
    id bigserial primary key,
    financial_statement_id bigint not null,
    recipient_email varchar(255) not null,
    format varchar(32) not null,
    frequency varchar(32) not null,
    hour_of_day integer not null,
    minute_of_hour integer not null,
    day_of_week integer,
    day_of_month integer,
    timezone varchar(255) not null,
    active boolean not null,
    next_run_at timestamp with time zone not null,
    last_run_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_financial_statement_email_schedule_statement
        foreign key (financial_statement_id) references financial_statement (id)
);

create index idx_financial_statement_email_schedule_next_run
    on financial_statement_email_schedule (active, next_run_at);
create index idx_financial_statement_email_schedule_statement_id
    on financial_statement_email_schedule (financial_statement_id);

create table financial_statement_history (
    id bigserial primary key,
    state varchar(40) not null,
    delivery_way varchar(40) not null,
    created_at timestamp with time zone not null,
    financial_statement_id bigint not null,
    constraint fk_financial_statement_history_statement
        foreign key (financial_statement_id) references financial_statement (id)
);

create index idx_financial_statement_history_created_at on financial_statement_history (created_at);
create index idx_financial_statement_history_statement_id on financial_statement_history (financial_statement_id);

create table financial_statement_log (
    id bigserial primary key,
    event_type varchar(60) not null,
    message varchar(500) not null,
    icon varchar(80),
    color varchar(30),
    created_at timestamp with time zone not null,
    financial_statement_id bigint not null,
    constraint fk_financial_statement_log_statement
        foreign key (financial_statement_id) references financial_statement (id)
);

create index idx_financial_statement_log_created_at on financial_statement_log (created_at);
create index idx_financial_statement_log_statement_id on financial_statement_log (financial_statement_id);

create table financial_statement_template (
    id bigserial primary key,
    ent_id varchar(255) not null,
    name varchar(255) not null,
    path_logotype varchar(255),
    alignment varchar(255),
    font varchar(255),
    font_size integer,
    main_color varchar(255),
    is_default boolean not null,
    created_at timestamp with time zone not null
);

create index idx_financial_statement_template_ent_id on financial_statement_template (ent_id);
create index idx_financial_statement_template_default on financial_statement_template (ent_id, is_default);
