-- Drop and recreate all domain tables with uuid id columns (dev seed only)
drop table if exists grade_modification;
drop table if exists grade;
drop table if exists student_group;
drop table if exists exam;
drop table if exists course_group;
drop table if exists course_teacher;
drop table if exists course;
drop table if exists "group";
drop table if exists "user";
drop table if exists promotion;

create table promotion
(
    id   uuid not null primary key,
    ref  varchar(255) not null unique,
    year integer not null
);

create table "user"
(
    id         uuid not null primary key,
    ref        varchar(255) not null,
    first_name varchar(255) not null,
    last_name  varchar(255) not null,
    email      varchar(255) not null unique,
    role       varchar(255) not null
);

create table "group"
(
    id           uuid not null primary key,
    ref          varchar(255) not null unique,
    promotion_id uuid not null
);

create table course
(
    id           uuid not null primary key,
    credits      integer not null,
    promotion_id uuid not null,
    ref          varchar(255) not null unique,
    title        varchar(255) not null
);

create table course_teacher
(
    id         uuid not null primary key,
    course_id  uuid not null,
    teacher_id uuid not null
);

create table course_group
(
    id        uuid not null primary key,
    course_id uuid not null,
    group_id  uuid not null
);

create table exam
(
    id          uuid not null primary key,
    coefficient double precision not null,
    course_id   uuid not null,
    date_exam   timestamp with time zone not null,
    ref         varchar(255) not null unique
);

create table student_group
(
    id         uuid not null primary key,
    end_date   timestamp with time zone,
    group_id   uuid not null,
    start_date timestamp with time zone not null,
    student_id uuid not null
);

create table grade
(
    id          uuid not null primary key,
    comment     varchar(255),
    exam_id     uuid not null,
    modified_at timestamp with time zone not null,
    modified_by uuid not null,
    student_id  uuid not null,
    value       double precision not null
);

create table grade_modification
(
    id          uuid not null primary key,
    grade_id    uuid not null,
    modified_at timestamp with time zone not null,
    modified_by uuid not null,
    new_value   double precision not null,
    old_value   double precision not null,
    reason      varchar(255) not null
);
