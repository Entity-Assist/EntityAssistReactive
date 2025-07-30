module com.entityassist {

    exports com.entityassist;
    exports com.entityassist.services.entities;
    exports com.entityassist.services.querybuilders;
    exports com.entityassist.converters;
    exports com.entityassist.enumerations;
    exports com.entityassist.querybuilder;
    exports com.entityassist.exceptions;
    exports com.entityassist.querybuilder.builders;

    requires transitive com.guicedee.vertxpersistence;

    requires java.naming;
    requires java.sql;

    requires transitive jakarta.persistence;
    requires jakarta.xml.bind;
    requires static lombok;

    requires transitive io.vertx.sql.client.pg;
    requires transitive org.hibernate.reactive;
    requires io.smallrye.mutiny;
    requires transitive org.hibernate.orm.core;

    opens com.entityassist to org.hibernate.orm.core, com.fasterxml.jackson.databind, com.google.guice, org.hibernate.validator;
}
