create table if not exists grade_modification
(
    id          uuid not null primary key,
    grade_id    uuid not null,
    modified_at timestamp with time zone not null,
    modified_by uuid not null,
    new_value   double precision not null,
    old_value   double precision not null,
    reason      varchar(255) not null
);
