import com.guicedee.guicedinjection.interfaces.IGuiceModule;

module entity.assist.test {

    requires transitive com.entityassist;
    requires transitive com.guicedee.vertxpersistence;

    requires org.junit.jupiter.api;
    requires junit;

    requires jakarta.xml.bind;
    requires jakarta.persistence;

    requires org.hibernate.reactive;
    requires io.smallrye.mutiny;
    requires com.google.guice;
    requires static lombok;

    requires org.testcontainers;
    requires io.vertx.sql.client.pg;

    opens com.test to org.junit.platform.commons,org.hibernate.orm.core,com.google.guice,net.bytebuddy,com.entityassist;

    provides com.guicedee.guicedinjection.interfaces.IGuiceModule with com.test.EntityAssistReactiveDBModule;
}
