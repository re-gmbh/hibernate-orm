/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds on {@link org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf2.InstantiationTests}
 * attempting to map an interface.
 *
 * At the moment this does not work, as Hibernate fails without setters
 */
@DomainModel( annotatedClasses = { Person.class, NameImpl.class } )
@SessionFactory
@JiraKey( "HHH-14950" )
public class InstantiationTests {
	@Test
	public void modelTest(DomainModelScope scope) {
		scope.withHierarchy( Person.class, (personMapping) -> {
			final Property name = personMapping.getProperty( "name" );
			final Component nameMapping = (Component) name.getValue();
			assertThat( nameMapping.getPropertySpan() ).isEqualTo( 2 );

			final Property aliases = personMapping.getProperty( "aliases" );
			final Component aliasMapping = (Component) ( (Collection) aliases.getValue() ).getElement();
			assertThat( aliasMapping.getPropertySpan() ).isEqualTo( 2 );
		});
	}

	@Test
	@FailureExpected( jiraKey = "HHH-14950", reason = "Model has no setters, which is not supported" )
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Person mick = new Person( 1, new NameImpl( "Mick", "Jagger" ) );
			session.persist( mick );

			final Person john = new Person( 2, new NameImpl( "John", "Doe" ) );
			john.addAlias( new NameImpl( "Jon", "Doe" ) );
			session.persist( john );
		} );
		scope.inTransaction( (session) -> {
			final Person mick = session.createQuery( "from Person where id = 1", Person.class ).uniqueResult();
			assertThat( mick.getName().getFirstName() ).isEqualTo( "Mick" );
		} );
		scope.inTransaction( (session) -> {
			final Person john = session.createQuery( "from Person p join fetch p.aliases where p.id = 2", Person.class ).uniqueResult();
			assertThat( john.getName().getFirstName() ).isEqualTo( "John" );
			assertThat( john.getAliases() ).hasSize( 1 );
			final Name alias = john.getAliases().iterator().next();
			assertThat( alias.getFirstName() ).isEqualTo( "Jon" );
		} );
	}
}
