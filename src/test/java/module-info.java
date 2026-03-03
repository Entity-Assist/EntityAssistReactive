import com.guicedee.client.services.lifecycle.IGuiceModule;

module entity.assist.test {

    requires transitive com.entityassist;
    requires transitive com.guicedee.persistence;

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
    requires com.ongres.scram.client;

    opens com.test to org.junit.platform.commons,org.hibernate.orm.core,com.google.guice,net.bytebuddy,com.entityassist;

    provides IGuiceModule with com.test.EntityAssistReactiveDBModule;
}
