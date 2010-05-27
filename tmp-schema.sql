drop table roi_slide;
drop table annotation;
drop table roi;
drop table author;

create table author (
    id int auto_increment primary key,
    name text not null
  )
  engine = InnoDB;

create table roi (
    id int auto_increment primary key,
    path text not null,
    author_id int not null references author,
    timestamp timestamp not null default current_timestamp
  )
  engine = InnoDB;

create table annotation (
    id int auto_increment primary key,
    roi_id int not null references roi,
    author_id int not null references author,
    timestamp timestamp not null default current_timestamp,
    text text not null
  )
  engine = InnoDB;

create table roi_slide (
    id int auto_increment primary key,
    quickhash1 text not null,
    roi_id int not null references roi,
    deleted bit not null default 0
  )
  engine = InnoDB;




insert into author (name) values ('Adam Goode');
insert into author (name) values ('Satya');
