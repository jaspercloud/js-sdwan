create table static_route
(
    id          bigint       NOT NULL generated by default as identity,
    destination varchar(128) NOT NULL,
    mesh_id     bigint       NOT NULL,
    remark      varchar(128),
    constraint static_route_id_key PRIMARY KEY (id)
);

create table node
(
    id          bigint       NOT NULL generated by default as identity,
    node_type   int          NOT NULL,
    vip         varchar(128) NOT NULL,
    mac_address varchar(32)  NOT NULL,
    remark      varchar(128),
    constraint node_id_key PRIMARY KEY (id)
);

create unique index node_vip_uindex
    on node (vip);

create unique index node_mac_uindex
    on node (mac_address);
