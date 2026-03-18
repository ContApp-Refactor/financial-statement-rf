create table financial_statement_annotation (
    id bigserial primary key,
    financial_statement_id bigint not null,
    text varchar(2000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_financial_statement_annotation_statement
        foreign key (financial_statement_id) references financial_statement (id)
);

create index idx_financial_statement_annotation_statement_id
    on financial_statement_annotation (financial_statement_id);
create index idx_financial_statement_annotation_created_at
    on financial_statement_annotation (created_at);
